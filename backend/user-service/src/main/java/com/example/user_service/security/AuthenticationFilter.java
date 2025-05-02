package com.example.user_service.security;

import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.Response;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.User;
import com.example.user_service.enumeration.LoginType;
import com.example.user_service.enumeration.TokenType;
import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.RequestUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class AuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private final UserService userService;
    private final JwtService jwtService;

    protected AuthenticationFilter(
            @Value("${api.endpoint.user.login}") String loginPath,
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService) {
        super(new AntPathRequestMatcher(loginPath, POST.name()), authenticationManager);
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        log.info("Attempting authentication into filter");
        try {
            LoginRequest loginRequest = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    .readValue(request.getInputStream(), LoginRequest.class);
            userService.updateLoginAttempt(loginRequest.getEmail(), LoginType.LOGIN_ATTEMPT, request);

            return getAuthenticationManager().authenticate(ApiAuthentication.unauthenticated(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()));
        } catch (Exception ex) {
            log.error("Authentication into filter failed: {}", ex.getMessage());
            RequestUtils.handlerErrorResponse(request, response, ex);
            return null;
        }

    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();
        userService.updateLoginAttempt(user.getEmail(), LoginType.LOGIN_SUCCESS, request);
        Response httpResponse = user.isMfa() ? sendQrCode(request, user) : sendResponse(request, response, user);
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setStatus(OK.value());
        OutputStream out = response.getOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, httpResponse);
        out.flush();
    }

    private Response sendResponse(HttpServletRequest request, HttpServletResponse response, User user) {
        jwtService.addCookie(response, user, TokenType.ACCESS);
        jwtService.addCookie(response, user, TokenType.REFRESH);
        return RequestUtils.getResponse(request, Map.of("user", user), "Login successful", OK);
    }

    private Response sendQrCode(HttpServletRequest request, User user) {
        return RequestUtils.getResponse(request, Map.of("user", user), "Please enter QR code", OK);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        log.error("Authentication failed: {}", failed.getMessage());
        RequestUtils.handlerErrorResponse(request, response, failed);
    }
}

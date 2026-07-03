package com.example.user_service.security;

import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.Response;
import com.example.user_service.domain.RequestContext;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
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
        LoginRequest loginRequest = null;
        try {
            loginRequest = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
                    .readValue(request.getInputStream(), LoginRequest.class);
            userService.updateLoginAttempt(loginRequest.getEmail(), LoginType.LOGIN_ATTEMPT, request);

            return getAuthenticationManager().authenticate(ApiAuthentication.unauthenticated(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()));
        } catch (Exception ex) {
            recordLoginFailure(loginRequest, ex);
            log.error("Authentication into filter failed: {}", ex.getMessage());
            RequestUtils.handlerErrorResponse(request, response, ex);
            RequestContext.clear();
            return null;
        }

    }

    private void recordLoginFailure(LoginRequest loginRequest, Exception exception) {
        if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
            return;
        }
        try {
            userService.recordLoginFailed(loginRequest.getEmail(), failureReason(exception));
        } catch (RuntimeException outboxException) {
            log.warn("Unable to record failed login audit event: category={}",
                    outboxException.getClass().getSimpleName());
        }
    }

    private String failureReason(Exception exception) {
        if (exception instanceof BadCredentialsException) {
            return "BAD_CREDENTIALS";
        }
        if (exception instanceof LockedException) {
            return "ACCOUNT_LOCKED";
        }
        if (exception instanceof DisabledException) {
            return "ACCOUNT_DISABLED";
        }
        if (exception instanceof CredentialsExpiredException) {
            return "CREDENTIALS_EXPIRED";
        }
        return "AUTHENTICATION_FAILED";
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        try {
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
        } finally {
            RequestContext.clear();
        }
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
        try {
            RequestUtils.handlerErrorResponse(request, response, failed);
        } finally {
            RequestContext.clear();
        }
    }
}

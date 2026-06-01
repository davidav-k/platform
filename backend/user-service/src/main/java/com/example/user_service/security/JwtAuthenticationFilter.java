package com.example.user_service.security;

import com.example.user_service.domain.RequestContext;
import com.example.user_service.dto.User;
import com.example.user_service.enumeration.TokenType;
import com.example.user_service.service.JwtService;
import com.example.user_service.domain.TokenData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Optional<String> token = jwtService.extractToken(request, TokenType.ACCESS.getValue());

            if (token.isPresent()) {
                TokenData tokenData = jwtService.getTokenData(token.get(), data -> data);
                User user = tokenData.getUser();

                if (tokenData.isValid()) {
                    RequestContext.setUserId(user.getId());
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            user, null, tokenData.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}

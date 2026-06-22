package com.example.task_service.notification;


import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;


@Component
public class CurrentRequestAccessTokenProvider {

    public Optional<String> currentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getCredentials() instanceof String token) {
                return Optional.of(token);
            }
        }

        return Optional.empty();
    }
}

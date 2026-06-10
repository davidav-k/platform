package com.example.task_service.notification;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CurrentRequestAccessTokenProvider {

    private static final String ACCESS_TOKEN_COOKIE = "access-token";
    private static final String BEARER_PREFIX = "Bearer ";

    public Optional<String> currentAccessToken() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
            .filter(header -> header.startsWith(BEARER_PREFIX))
            .map(header -> header.substring(BEARER_PREFIX.length()))
            .or(() -> accessTokenCookie(request));
    }

    private Optional<String> accessTokenCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
            .stream()
            .flatMap(Arrays::stream)
            .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}

package com.example.user_service.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
public class JwrConfig {
    @Value("${jwt.expiration-seconds}")
    private Long expirationSeconds;
    @Value("${jwt.secret}")
    private String secret;
    @Value("${auth.cookie.secure}")
    private boolean cookieSecure;
    @Value("${auth.cookie.same-site}")
    private String cookieSameSite;
    @Value("${auth.cookie.access-max-age-seconds}")
    private int accessCookieMaxAgeSeconds;
    @Value("${auth.cookie.refresh-max-age-seconds}")
    private int refreshCookieMaxAgeSeconds;
}

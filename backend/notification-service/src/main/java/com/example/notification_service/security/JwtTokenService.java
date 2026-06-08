package com.example.notification_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String AUTHORITIES = "authorities";
    private static final String ROLE = "role";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String AUTHORITY_DELIMITER = ",";

    private final SecretKey key;

    public JwtTokenService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public JwtAuthenticationData parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtAuthenticationData(
                UUID.fromString(claims.getSubject()),
                claims.getSubject(),
                authorities(claims)
        );
    }

    private List<GrantedAuthority> authorities(Claims claims) {
        StringJoiner grantedAuthorities = new StringJoiner(AUTHORITY_DELIMITER);
        Optional.ofNullable(claims.get(AUTHORITIES, String.class))
                .ifPresent(grantedAuthorities::add);
        Optional.ofNullable(claims.get(ROLE, String.class))
                .map(role -> ROLE_PREFIX + role)
                .ifPresent(grantedAuthorities::add);

        String authorityString = grantedAuthorities.toString();
        return authorityString.isBlank()
                ? List.of()
                : AuthorityUtils.commaSeparatedStringToAuthorityList(authorityString);
    }

    public record JwtAuthenticationData(
            UUID userId,
            String username,
            List<GrantedAuthority> authorities
    ) {
    }
}

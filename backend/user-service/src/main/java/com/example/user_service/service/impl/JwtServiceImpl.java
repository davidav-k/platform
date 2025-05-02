package com.example.user_service.service.impl;

import com.example.user_service.domain.Token;
import com.example.user_service.domain.TokenData;
import com.example.user_service.dto.User;
import com.example.user_service.enumeration.TokenType;
import com.example.user_service.security.JwrConfig;
import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.lang.Supplier;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.example.user_service.constant.Constants.*;
import static com.example.user_service.enumeration.TokenType.ACCESS;
import static com.example.user_service.enumeration.TokenType.REFRESH;
import static io.jsonwebtoken.Header.JWT_TYPE;
import static io.jsonwebtoken.Header.TYPE;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtServiceImpl extends JwrConfig implements JwtService {

    private final UserService userService;

    private final Supplier<SecretKey> key = () -> Keys.hmacShaKeyFor(Decoders.BASE64.decode(getSecret()));

    private final Function<String, Claims> claimsFunction = token ->
            Jwts.parser()
                    .verifyWith(key.get())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

    private final Function<String, String> subject = token -> getClaimsValue(token, Claims::getSubject);

    private final BiFunction<HttpServletRequest, String, Optional<String>> extractToken = (request, cookieName) ->
            Optional.of(stream(request.getCookies() == null ? new Cookie[]{new Cookie(EMPTY_VALUE, EMPTY_VALUE)} : request.getCookies())
                            .filter(cookie -> Objects.equals(cookieName, cookie.getName()))
                            .map(Cookie::getValue)
                            .findAny())
                    .orElse(empty());

    private final BiFunction<HttpServletRequest, String, Optional<Cookie>> extractCookie = (request, cookieName) ->
            Optional.of(stream(request.getCookies() == null ? new Cookie[]{new Cookie(EMPTY_VALUE, EMPTY_VALUE)} : request.getCookies())
                            .filter(cookie -> Objects.equals(cookieName, cookie.getName()))
                            .findAny())
                    .orElse(empty());

    private final Supplier<JwtBuilder> builder = () -> Jwts.builder()
            .header().add(Map.of(TYPE, JWT_TYPE))
            .and()
            .audience().add(GET_ARRAYS_LLC)
            .and()
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(Instant.now()))
            .notBefore(new Date())
            .signWith(key.get(), Jwts.SIG.HS512);

    private final BiFunction<User, TokenType, String> buildToken = (user, tokenType) ->
            Objects.equals(tokenType, ACCESS) ?
                    builder.get()
                            .subject(user.getUserId())
                            .claim(AUTHORITIES, user.getAuthorities())
                            .claim(ROLE, user.getRole())
                            .expiration(Date.from(Instant.now().plusSeconds(getExpiration())))
                            .compact() :
                    builder.get()
                            .subject(user.getUserId())
                            .expiration(Date.from(Instant.now().plusSeconds(getExpiration())))
                            .compact();

    private final TriConsumer<HttpServletResponse, User, TokenType> addCookie = (response, user, tokenType) -> {
        boolean isSecure = response.getHeader("X-Forwarded-Proto") != null && response.getHeader("X-Forwarded-Proto").equals("https");

        switch (tokenType) {
            case ACCESS -> {
                var accessToken = createToken(user, Token::getAccess);
                var cookie = new Cookie(tokenType.getValue(), accessToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(isSecure);
                cookie.setMaxAge(10 * 60);  // 10 мин
                cookie.setPath("/");
                cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
                response.addCookie(cookie);
                log.info("🍪 Cookie: {} = {} | Secure: {} | SameSite: {}", tokenType.getValue(), accessToken, cookie.getSecure(), cookie.getAttribute("SameSite"));

            }
            case REFRESH -> {
                var refreshToken = createToken(user, Token::getRefresh);
                Cookie cookie = new Cookie(tokenType.getValue(), refreshToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(isSecure);
                cookie.setMaxAge(2 * 60 * 60); // 2 часа
                cookie.setPath("/");
                cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
                response.addCookie(cookie);
                log.info("🍪 Cookie: {} = {} | Secure: {} | SameSite: {}", tokenType.getValue(), refreshToken, cookie.getSecure(), cookie.getAttribute("SameSite"));

            }
        }
    };


    private <T> T getClaimsValue(String token, Function<Claims, T> claims) {
        return claimsFunction.andThen(claims).apply(token);
    }

    public Function<String, List<GrantedAuthority>> authorities = token -> commaSeparatedStringToAuthorityList(
            new StringJoiner(AUTHORITY_DELIMITER)
                    .add(claimsFunction.apply(token).get(AUTHORITIES, String.class))
                    .add((ROLE_PREFIX + claimsFunction.apply(token).get(ROLE, String.class))).toString()
    );

    @Override
    public String createToken(User user, Function<Token, String> tokenFunction) {
        Token token = Token.builder()
                .access(buildToken.apply(user, ACCESS))
                .refresh(buildToken.apply(user, REFRESH))
                .build();
        return tokenFunction.apply(token);
    }

    @Override
    public Optional<String> extractToken(HttpServletRequest request, String cookieName) {
        return extractToken.apply(request, cookieName);
    }

    @Override
    public void addCookie(HttpServletResponse response, User user, TokenType tokenType) {
        log.info("addCookie before accept");
        addCookie.accept(response, user, tokenType);
    }

    @Override
    public <T> T getTokenData(String token, Function<TokenData, T> tokenFunction) {
        return tokenFunction.apply(
                TokenData.builder()
                        .isValid(Objects.equals(userService.getUserByUserId(subject.apply(token)).getUserId(), claimsFunction.apply(token).getSubject()))
                        .authorities(authorities.apply(token))
                        .claims(claimsFunction.apply(token))
                        .user(userService.getUserByUserId(subject.apply(token)))

                        .build());
    }

    @Override
    public void removeCookie(HttpServletRequest request, HttpServletResponse response, TokenType tokenType) {
        extractCookie.apply(request, tokenType.getValue()).ifPresent(cookie -> {
            cookie.setValue(EMPTY_VALUE);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        });
    }
}

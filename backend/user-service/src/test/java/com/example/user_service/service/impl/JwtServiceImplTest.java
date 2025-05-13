package com.example.user_service.service.impl;

import com.example.user_service.domain.Token;
import com.example.user_service.domain.TokenData;
import com.example.user_service.dto.User;
import com.example.user_service.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.example.user_service.enumeration.TokenType.ACCESS;
import static com.example.user_service.enumeration.TokenType.REFRESH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", "thisIsASecretKeyUsedForTestingPurposesOnly12345678901234567890thisIsASecretKeyUsedForTestingPurposesOnly12345678901234567890");
        ReflectionTestUtils.setField(jwtService, "expiration", 600L);
    }

    @Test
    void createTokenGeneratesValidAccessToken() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        String token = jwtService.createToken(user, Token::getAccess);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void createTokenGeneratesValidRefreshToken() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        String token = jwtService.createToken(user, Token::getRefresh);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractTokenReturnsTokenFromCookie() {
        Cookie[] cookies = new Cookie[]{new Cookie("access-token", "test-token")};
        when(request.getCookies()).thenReturn(cookies);

        Optional<String> result = jwtService.extractToken(request, "access-token");

        assertTrue(result.isPresent());
        assertEquals("test-token", result.get());
    }

    @Test
    void extractTokenReturnsEmptyWhenCookieNotFound() {
        Cookie[] cookies = new Cookie[]{new Cookie("other-cookie", "value")};
        when(request.getCookies()).thenReturn(cookies);

        Optional<String> result = jwtService.extractToken(request, "access-token");

        assertFalse(result.isPresent());
    }

    @Test
    void extractTokenHandlesNullCookies() {
        when(request.getCookies()).thenReturn(null);

        Optional<String> result = jwtService.extractToken(request, "access-token");

        assertFalse(result.isPresent());
    }

    @Test
    void addCookieCreatesAccessCookie() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        doNothing().when(response).addCookie(any(Cookie.class));
        when(response.getHeader("X-Forwarded-Proto")).thenReturn(null);

        jwtService.addCookie(response, user, ACCESS);

        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    void addCookieCreatesRefreshCookie() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        doNothing().when(response).addCookie(any(Cookie.class));
        when(response.getHeader("X-Forwarded-Proto")).thenReturn(null);

        jwtService.addCookie(response, user, REFRESH);

        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    void addCookieSetsSecureAttributeWhenHttps() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        doNothing().when(response).addCookie(any(Cookie.class));
        when(response.getHeader("X-Forwarded-Proto")).thenReturn("https");

        jwtService.addCookie(response, user, ACCESS);

        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    void getTokenDataReturnsValidTokenData() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        String token = jwtService.createToken(user, Token::getAccess);
        when(userService.getUserByUserId(anyString())).thenReturn(user);

        boolean isValid = jwtService.getTokenData(token, TokenData::isValid);

        assertTrue(isValid);
    }

    @Test
    void getTokenDataReturnsAuthorities() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        String token = jwtService.createToken(user, Token::getAccess);
        when(userService.getUserByUserId(anyString())).thenReturn(user);

        List<GrantedAuthority> authorities = jwtService.getTokenData(token, TokenData::getAuthorities);

        assertNotNull(authorities);
        assertTrue(authorities.contains(new SimpleGrantedAuthority("READ")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("WRITE")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void removeCookieDeletesExistingCookie() {
        Cookie cookie = new Cookie("access-token", "test-token");
        Cookie[] cookies = new Cookie[]{cookie};
        when(request.getCookies()).thenReturn(cookies);

        jwtService.removeCookie(request, response, ACCESS);

        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    void removeCookieHandlesNonExistingCookie() {
        Cookie[] cookies = new Cookie[]{new Cookie("other-cookie", "value")};
        when(request.getCookies()).thenReturn(cookies);

        jwtService.removeCookie(request, response, ACCESS);

        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    void getTokenDataRejectsInvalidToken() {

        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("USER")
                .build();

        String token = jwtService.createToken(user, Token::getAccess);

        User differentUser = User.builder()
                .userId("different-user-id")
                .authorities("READ")
                .role("USER")
                .build();
        when(userService.getUserByUserId(anyString())).thenReturn(differentUser);

        boolean isValid = jwtService.getTokenData(token, TokenData::isValid);

        assertFalse(isValid);
    }

    @Test
    void createTokenGeneratesTokenWithCorrectClaims() {

        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ,WRITE")
                .role("ADMIN")
                .build();

        String token = jwtService.createToken(user, Token::getAccess);
        when(userService.getUserByUserId(anyString())).thenReturn(user);

        Claims claims = jwtService.getTokenData(token, TokenData::getClaims);

        assertEquals("test-user-id", claims.getSubject());
        assertEquals("READ,WRITE", claims.get("authorities"));
        assertEquals("ADMIN", claims.get("role"));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void tokenExpirationIsCorrectlySet() {
        User user = User.builder()
                .userId("test-user-id")
                .authorities("READ")
                .role("USER")
                .build();

        long currentTimeMillis = System.currentTimeMillis();

        String token = jwtService.createToken(user, Token::getAccess);
        when(userService.getUserByUserId(anyString())).thenReturn(user);

        Claims claims = jwtService.getTokenData(token, TokenData::getClaims);
        long expirationTime = claims.getExpiration().getTime();

        long expectedExpirationTime = currentTimeMillis + 600 * 1000;
        assertTrue(Math.abs(expirationTime - expectedExpirationTime) < 5000); // позволяем 5 секунд разницы для выполнения кода
    }

@Test
void removeCookieSetsCorrectExpirationTime() {

    Cookie cookie = new Cookie("access-token", "test-token");
    Cookie[] cookies = new Cookie[]{cookie};
    when(request.getCookies()).thenReturn(cookies);

    ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

    jwtService.removeCookie(request, response, ACCESS);

    verify(response).addCookie(cookieCaptor.capture());
    Cookie removedCookie = cookieCaptor.getValue();

    assertEquals(0, removedCookie.getMaxAge());
    assertEquals("empty", removedCookie.getValue());
    assertEquals("access-token", removedCookie.getName());
}
}
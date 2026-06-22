package com.example.task_service.notification;


import com.example.task_service.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CurrentRequestAccessTokenProviderTest {

    private final CurrentRequestAccessTokenProvider accessTokenProvider = new CurrentRequestAccessTokenProvider();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void currentAccessToken_ShouldReturnToken_WhenUserIsAuthenticated() {
        String expectedToken = "valid.jwt.token.string";
        AuthenticatedUser principal = new AuthenticatedUser(java.util.UUID.randomUUID(), "test_user");


        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                expectedToken,
                Collections.emptyList()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> actualToken = accessTokenProvider.currentAccessToken();

        assertTrue(actualToken.isPresent());
        assertEquals(expectedToken, actualToken.get());
    }

    @Test
    void currentAccessToken_ShouldReturnEmpty_WhenSecurityContextIsEmpty() {

        Optional<String> actualToken = accessTokenProvider.currentAccessToken();


        assertTrue(actualToken.isEmpty());
    }

    @Test
    void currentAccessToken_ShouldReturnEmpty_WhenAuthenticationIsUnknownType() {

        Authentication mockAuthentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);

        Optional<String> actualToken = accessTokenProvider.currentAccessToken();

        assertTrue(actualToken.isEmpty());
    }

    @Test
    void currentAccessToken_ShouldReturnEmpty_WhenCredentialsAreNotString() {

        AuthenticatedUser principal = new AuthenticatedUser(java.util.UUID.randomUUID(), "test_user");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> actualToken = accessTokenProvider.currentAccessToken();

        assertTrue(actualToken.isEmpty());
    }
}

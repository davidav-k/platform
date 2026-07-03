package com.example.user_service.security;

import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {

    private AuthenticationManager authenticationManager;
    private UserService userService;
    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userService = mock(UserService.class);
        authenticationFilter = new AuthenticationFilter(
                "/api/v1/user/login",
                authenticationManager,
                userService,
                mock(JwtService.class)
        );
    }

    @Test
    void failedLoginRecordsSafeAuditCategoryWithoutPassword() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/user/login");
        request.setContentType("application/json");
        request.setContent("""
                {
                  "email": "user@example.com",
                  "password": "WrongPassword123"
                }
                """.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Email and/or password is incorrect"));

        assertThat(authenticationFilter.attemptAuthentication(request, response)).isNull();

        verify(userService).recordLoginFailed("user@example.com", "BAD_CREDENTIALS");
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).doesNotContain("WrongPassword123");
    }
}

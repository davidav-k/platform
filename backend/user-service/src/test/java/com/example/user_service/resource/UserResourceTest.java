package com.example.user_service.resource;

import com.example.user_service.UserServiceApplication;
import com.example.user_service.config.TestContainersConfig;
import com.example.user_service.config.TestEmailConfig;
import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.Response;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.User;
import com.example.user_service.dto.UserRequest;
import com.example.user_service.enumeration.TokenType;
import com.example.user_service.exception.ExceptionHandlerAdvice;
import com.example.user_service.service.JwtService;
import com.example.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersConfig.Initializer.class,
        classes = {UserServiceApplication.class, TestSecurityConfig.class, TestEmailConfig.class})
public class UserResourceTest {
    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserResource userResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(userResource)
                .setControllerAdvice(new ExceptionHandlerAdvice())
                .build();
    }

    @Test
    void saveUserCreatesNewUserSuccessfully() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstName("John");
        userRequest.setLastName("Doe");
        userRequest.setEmail("john@example.com");
        userRequest.setPassword("password");

        doNothing().when(userService).createUser(anyString(), anyString(), anyString(), anyString());
        when(request.getRequestURI()).thenReturn("/api/v1/user/register");

        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created. Check your email to enable your account"));
    }

    @Test
    void verifyNewUserAccountSuccessfully() throws Exception {
        String key = "verification-key";
        doNothing().when(userService).verifyAccountKey(key);
        when(request.getRequestURI()).thenReturn("/api/v1/user/verify/account");

        mockMvc.perform(get("/api/v1/user/verify/account")
                .param("key", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account verified successfully."));
    }

@Test
void loginUserSuccessfully() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail("john@example.com");
    loginRequest.setPassword("password");

    User mockUser = User.builder().id(1L).email("john@example.com").build();

    ApiAuthentication authentication = mock(ApiAuthentication.class);
    when(authentication.getPrincipal()).thenReturn(mockUser);

    when(userService.authenticateUser(anyString(), anyString(), any(HttpServletRequest.class)))
            .thenReturn(authentication);
    when(request.getRequestURI()).thenReturn("/api/v1/user/login");
    doNothing().when(jwtService).addCookie(any(), any(), any());

    ResponseEntity<Response> response = userResource.loginUser(loginRequest, request, this.response);

    verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.ACCESS));
    verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.REFRESH));
    assert response.getStatusCode() == HttpStatus.OK;
    assert response.getBody().message().contains("Login successful");
}

    @Test
    void enableMfaSuccessfully() {
        String email = "john@example.com";
        doNothing().when(userService).enableMfa(email);
        when(request.getRequestURI()).thenReturn("/api/v1/user/enable-mfa");

        ResponseEntity<Response> response = userResource.enableMfa(email, request);

        verify(userService, times(1)).enableMfa(email);
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("MFA enabled successfully");
    }

    @Test
    void verifyMfaSuccessfully() {
        String email = "john@example.com";
        int code = 123456;
        User mockUser = User.builder().email(email).build();

        when(userService.verifyMfa(email, code)).thenReturn(true);
        when(userService.getUserByEmail(email)).thenReturn(mockUser);
        when(request.getRequestURI()).thenReturn("/api/v1/user/verify-mfa");
        doNothing().when(jwtService).addCookie(any(), any(), any());

        ResponseEntity<Response> response = userResource.verifyMfa(email, code, request, this.response);

        verify(userService, times(1)).verifyMfa(email, code);
        verify(userService, times(1)).getUserByEmail(email);
        verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.ACCESS));
        verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.REFRESH));
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("MFA verification successful");
    }

    @Test
    void verifyMfaFailsWithInvalidCode() {
        String email = "john@example.com";
        int code = 123456;

        when(userService.verifyMfa(email, code)).thenReturn(false);
        when(request.getRequestURI()).thenReturn("/api/v1/user/verify-mfa");

        ResponseEntity<Response> response = userResource.verifyMfa(email, code, request, this.response);

        verify(userService, times(1)).verifyMfa(email, code);
        verify(userService, never()).getUserByEmail(any());
        verify(jwtService, never()).addCookie(any(), any(), any());
        assert response.getStatusCode() == HttpStatus.FORBIDDEN;
        assert response.getBody().message().contains("Invalid MFA code");
    }

    @Test
    void unlockUserSuccessfully() {
        String email = "john@example.com";
        doNothing().when(userService).unlockedUser(email);
        when(request.getRequestURI()).thenReturn("/api/v1/user/unlock");

        ResponseEntity<Response> response = userResource.unlockUser(email, request);

        verify(userService, times(1)).unlockedUser(email);
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("User unlocked successfully");
    }

    @Test
    void lockUserSuccessfully() {
        String email = "john@example.com";
        doNothing().when(userService).lockedUser(email);
        when(request.getRequestURI()).thenReturn("/api/v1/user/lock");

        ResponseEntity<Response> response = userResource.lockUser(email, request);

        verify(userService, times(1)).lockedUser(email);
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("User locked successfully");
    }

@Test
void refreshTokensSuccessfully() {
    String refreshToken = "valid-refresh-token";
    User mockUser = User.builder().userId("user123").build();

    when(jwtService.extractToken(request, TokenType.REFRESH.getValue())).thenReturn(Optional.of(refreshToken));
    // Simply return the mockUser instead of trying to create a TokenData
    when(jwtService.getTokenData(eq(refreshToken), any())).thenReturn(mockUser);
    when(userService.getUserByUserId(mockUser.getUserId())).thenReturn(mockUser);
    when(request.getRequestURI()).thenReturn("/api/v1/user/refresh");
    doNothing().when(jwtService).addCookie(any(), any(), any());

    ResponseEntity<Response> response = userResource.refreshTokens(request, this.response);

    verify(jwtService, times(1)).extractToken(request, TokenType.REFRESH.getValue());
    verify(jwtService, times(1)).getTokenData(eq(refreshToken), any());
    verify(userService, times(1)).getUserByUserId(mockUser.getUserId());
    verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.ACCESS));
    verify(jwtService, times(1)).addCookie(any(), any(), eq(TokenType.REFRESH));
    assert response.getStatusCode() == HttpStatus.OK;
    assert response.getBody().message().contains("Tokens refreshed successfully");
}

    @Test
    void refreshTokensFailsWithMissingToken() {
        when(jwtService.extractToken(request, TokenType.REFRESH.getValue())).thenReturn(Optional.empty());
        when(request.getRequestURI()).thenReturn("/api/v1/user/refresh");

        ResponseEntity<Response> response = userResource.refreshTokens(request, this.response);

        verify(jwtService, times(1)).extractToken(request, TokenType.REFRESH.getValue());
        verify(jwtService, never()).getTokenData(any(), any());
        verify(userService, never()).getUserByUserId(any());
        verify(jwtService, never()).addCookie(any(), any(), any());
        assert response.getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert response.getBody().message().contains("Unauthorized access");
    }

    @Test
    void getUserProfileSuccessfully() {
        String accessToken = "valid-access-token";
        User mockUser = User.builder().userId("user123").build();

        when(jwtService.extractToken(request, TokenType.ACCESS.getValue())).thenReturn(Optional.of(accessToken));
        when(jwtService.getTokenData(eq(accessToken), any())).thenReturn(mockUser);
        when(userService.getUserByUserId(mockUser.getUserId())).thenReturn(mockUser);
        when(request.getRequestURI()).thenReturn("/api/v1/user/profile");

        ResponseEntity<Response> response = userResource.getUserProfile(request);

        verify(jwtService, times(1)).extractToken(request, TokenType.ACCESS.getValue());
        verify(jwtService, times(1)).getTokenData(eq(accessToken), any());
        verify(userService, times(1)).getUserByUserId(mockUser.getUserId());
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("User profile retrieved successfully");
    }

    @Test
    void getUserProfileFailsWithMissingToken() {
        when(jwtService.extractToken(request, TokenType.ACCESS.getValue())).thenReturn(Optional.empty());
        when(request.getRequestURI()).thenReturn("/api/v1/user/profile");

        ResponseEntity<Response> response = userResource.getUserProfile(request);

        verify(jwtService, times(1)).extractToken(request, TokenType.ACCESS.getValue());
        verify(jwtService, never()).getTokenData(any(), any());
        verify(userService, never()).getUserByUserId(any());
        assert response.getStatusCode() == HttpStatus.UNAUTHORIZED;
        assert response.getBody().message().contains("Unauthorized access");
    }

    @Test
    void updateUserSuccessfully() {
        Long userId = 1L;
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstName("Updated");
        userRequest.setLastName("Name");
        userRequest.setEmail("updated@example.com");

        doNothing().when(userService).updateUser(eq(userId), any(UserRequest.class));
        when(request.getRequestURI()).thenReturn("/api/v1/user/1");

        ResponseEntity<Response> response = userResource.updateUser(userId, userRequest, request);

        verify(userService, times(1)).updateUser(eq(userId), any(UserRequest.class));
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("User updated successfully");
    }

    @Test
    void changePasswordSuccessfully() {
        Long userId = 1L;
        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("oldPassword", "oldPass");
        passwordMap.put("newPassword", "newPass");
        passwordMap.put("confirmNewPassword", "newPass");

        doNothing().when(userService).changePassword(eq(userId), eq("oldPass"), eq("newPass"), eq("newPass"));
        when(request.getRequestURI()).thenReturn("/api/v1/user/password/1");

        ResponseEntity<Response> response = userResource.changePassword(userId, passwordMap, request);

        verify(userService, times(1)).changePassword(userId, "oldPass", "newPass", "newPass");
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("Changed password successfully");
    }

    @Test
    void deleteUserSuccessfully() {
        Long userId = 1L;
        Authentication authentication = mock(Authentication.class);

        doNothing().when(userService).deleteUser(userId, authentication);
        when(request.getRequestURI()).thenReturn("/api/v1/user/1");

        ResponseEntity<Response> response = userResource.deleteUser(userId, request, authentication);

        verify(userService, times(1)).deleteUser(userId, authentication);
        assert response.getStatusCode() == HttpStatus.OK;
        assert response.getBody().message().contains("User deleted successfully");
    }
}
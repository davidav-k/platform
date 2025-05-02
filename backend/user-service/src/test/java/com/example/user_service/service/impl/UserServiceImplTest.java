package com.example.user_service.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.user_service.cache.CacheStore;
import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.RequestContext;
import com.example.user_service.dto.User;
import com.example.user_service.dto.UserRequest;
import com.example.user_service.entity.*;
import com.example.user_service.enumeration.Authority;
import com.example.user_service.enumeration.LoginType;
import com.example.user_service.event.UserEvent;
import com.example.user_service.exception.ApiException;
import com.example.user_service.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private ConfirmationRepository confirmationRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private CacheStore<String, Integer> userCache;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private MfaServiceImpl mfaService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity userEntity;
    private RoleEntity roleEntity;
    private CredentialEntity credentialEntity;
    private ConfirmationEntity confirmationEntity;

@BeforeEach
void setup() {
    roleEntity = new RoleEntity();
    roleEntity.setName(Authority.USER.name());
    roleEntity.setAuthorities(Authority.USER);

    userEntity = new UserEntity();
    userEntity.setUserId("user123");
    userEntity.setEmail("test@example.com");
    userEntity.setFirstName("John");
    userEntity.setLastName("Doe");
    userEntity.setEnabled(true);
    userEntity.setAccountNonLocked(true);
    userEntity.setRole(roleEntity);
    userEntity.setLoginAttempts(0);
    userEntity.setLastLogin(LocalDate.now());

    credentialEntity = new CredentialEntity();
    credentialEntity.setUserEntity(userEntity);
    credentialEntity.setPassword("encodedPassword");

    confirmationEntity = new ConfirmationEntity();
    confirmationEntity.setUserEntity(userEntity);
    confirmationEntity.setKey("confirmationKey");

    try {
        setFieldValue(roleEntity, "id", 1L);
        setFieldValue(userEntity, "id", 1L);
        setFieldValue(credentialEntity, "id", 1L);
        setFieldValue(confirmationEntity, "id", 1L);
        for (Object entity : List.of(roleEntity, userEntity, credentialEntity, confirmationEntity)) {
            setFieldValue(entity, "createdAt", LocalDateTime.now());
            setFieldValue(entity, "updatedAt", LocalDateTime.now());
            setFieldValue(entity, "createdBy", 1L);
            setFieldValue(entity, "updatedBy", 1L);
        }

        RequestContext.setUserId(1L);
    } catch (Exception e) {
        fail("Failed to set up entity IDs: " + e.getMessage());
    }
}

private void setFieldValue(Object object, String fieldName, Object value) throws Exception {

    Class<?> currentClass = object.getClass();
    Field field = null;

    while (currentClass != null && field == null) {
        try {
            field = currentClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            currentClass = currentClass.getSuperclass();
        }
    }

    if (field == null) {
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + object.getClass().getName());
    }

    field.setAccessible(true);
    field.set(object, value);
}

    @Test
    void createUserWhenEmailAvailable() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase(Authority.USER.name())).thenReturn(Optional.of(roleEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        userService.createUser("John", "Doe", "test@example.com", "password");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(UserEntity.class));
        verify(credentialRepository).save(any(CredentialEntity.class));
        verify(confirmationRepository).save(any(ConfirmationEntity.class));
        verify(publisher).publishEvent(any(UserEvent.class));
    }

    @Test
    void createUserWhenEmailAlreadyExistsThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.createUser("John", "Doe", "test@example.com", "password"));

        assertEquals("User with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void verifyAccountKeyWhenKeyValid() {
        when(confirmationRepository.findByKey("confirmationKey")).thenReturn(Optional.of(confirmationEntity));
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));

        userService.verifyAccountKey("confirmationKey");

        assertTrue(userEntity.isEnabled());
        verify(userRepository).save(userEntity);
        verify(confirmationRepository).delete(confirmationEntity);
    }

    @Test
    void verifyAccountKeyWhenKeyInvalidThrowsException() {
        when(confirmationRepository.findByKey("invalidKey")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.verifyAccountKey("invalidKey"));

        assertEquals("Invalid key", exception.getMessage());
    }

    @Test
    void getUserByEmailWhenExisting() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(credentialRepository.getCredentialByUserEntityId(userEntity.getId())).thenReturn(Optional.of(credentialEntity));

        User user = userService.getUserByEmail("test@example.com");

        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void getUserByEmailWhenNotExistingThrowsException() {
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.getUserByEmail("nonexistent@example.com"));

        assertEquals("User by email not found", exception.getMessage());
    }

    @Test
    void authenticateUserWithValidCredentials() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(credentialRepository.getCredentialByUserEntityId(userEntity.getId())).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        ApiAuthentication authentication = userService.authenticateUser("test@example.com", "password", request);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        verify(loginHistoryRepository).save(any(LoginHistoryEntity.class));
    }

    @Test
    void authenticateUserWithInvalidPasswordThrowsException() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(credentialRepository.getCredentialByUserEntityId(userEntity.getId())).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.authenticateUser("test@example.com", "wrongPassword", request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(loginHistoryRepository).save(any(LoginHistoryEntity.class));
    }

    @Test
    void enableMfaForUser() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(mfaService.generateSecretKey()).thenReturn("secretKey");
        when(mfaService.generateQrCodeUrl(anyString(), anyString())).thenReturn("qrCodeUrl");

        userService.enableMfa("test@example.com");

        assertTrue(userEntity.isMfa());
        assertEquals("secretKey", userEntity.getQrCodeSecret());
        assertEquals("qrCodeUrl", userEntity.getQrCodeImageUrl());
        verify(userRepository).save(userEntity);
    }

    @Test
    void verifyMfaWithValidCode() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(mfaService.validateOtp("secretKey", 123456)).thenReturn(true);
        userEntity.setQrCodeSecret("secretKey");

        boolean result = userService.verifyMfa("test@example.com", 123456);

        assertTrue(result);
    }

    @Test
    void updateLoginAttemptLockAccountAfterTooManyAttempts() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(userCache.get("test@example.com")).thenReturn(6);

        userService.updateLoginAttempt("test@example.com", LoginType.LOGIN_ATTEMPT, request);

        assertEquals(1, userEntity.getLoginAttempts());
        assertFalse(userEntity.isAccountNonLocked());
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateLoginAttemptResetOnSuccessfulLogin() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        userService.updateLoginAttempt("test@example.com", LoginType.LOGIN_SUCCESS, request);

        assertEquals(0, userEntity.getLoginAttempts());
        assertTrue(userEntity.isAccountNonLocked());
        assertEquals(LocalDate.now(), userEntity.getLastLogin());
        verify(userCache).evict("test@example.com");
        verify(loginHistoryRepository).save(any(LoginHistoryEntity.class));
        verify(userRepository).save(userEntity);
    }

    @Test
    void unlockUserResetsLoginAttempts() {
        userEntity.setLoginAttempts(5);
        userEntity.setAccountNonLocked(false);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));

        userService.unlockedUser("test@example.com");

        assertEquals(0, userEntity.getLoginAttempts());
        assertTrue(userEntity.isAccountNonLocked());
        verify(userCache).evict("test@example.com");
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateUserWithValidData() {
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstName("Jane");
        userRequest.setLastName("Smith");
        userRequest.setEmail("test@example.com");
        userRequest.setPhone("123456789");
        userRequest.setBio("Updated bio");

        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        userService.updateUser(1L, userRequest);

        assertEquals("Jane", userEntity.getFirstName());
        assertEquals("Smith", userEntity.getLastName());
        assertEquals("123456789", userEntity.getPhone());
        assertEquals("Updated bio", userEntity.getBio());
        verify(userRepository).save(userEntity);
    }

    @Test
    void updateUserWithExistingEmailThrowsException() {
        UserRequest userRequest = new UserRequest();
        userRequest.setFirstName("Jane");
        userRequest.setLastName("Smith");
        userRequest.setEmail("different@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByEmail("different@example.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.updateUser(1L, userRequest));

        assertEquals("User with this email already exists", exception.getMessage());
    }

    @Test
    void changePasswordWithValidOldPassword() {
        when(credentialRepository.getCredentialByUserEntityId(1L)).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newEncodedPassword");

        userService.changePassword(1L, "oldPassword", "NewPassword123", "NewPassword123");

        assertEquals("newEncodedPassword", credentialEntity.getPassword());
    }

    @Test
    void changePasswordWithInvalidOldPasswordThrowsException() {
        when(credentialRepository.getCredentialByUserEntityId(1L)).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("wrongOldPassword", "encodedPassword")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
            () -> userService.changePassword(1L, "wrongOldPassword", "NewPassword123", "NewPassword123"));

        assertEquals("Old password is incorrect", exception.getMessage());
    }

    @Test
    void changePasswordWithNonMatchingNewPasswordsThrowsException() {
        when(credentialRepository.getCredentialByUserEntityId(1L)).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.changePassword(1L, "oldPassword", "NewPassword123", "DifferentPassword123"));

        assertEquals("New password and confirm new password do not match", exception.getMessage());
    }

    @Test
    void changePasswordWithWeakPasswordThrowsException() {
        when(credentialRepository.getCredentialByUserEntityId(1L)).thenReturn(Optional.of(credentialEntity));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
            () -> userService.changePassword(1L, "oldPassword", "weakpassword", "weakpassword"));

        assertEquals("New password does not conform to password policy", exception.getMessage());
    }

    @Test
    void getUserByUserIdWhenExisting() {
        when(userRepository.findUserByUserId("user123")).thenReturn(Optional.of(userEntity));
        when(credentialRepository.getCredentialByUserEntityId(userEntity.getId())).thenReturn(Optional.of(credentialEntity));

        User user = userService.getUserByUserId("user123");

        assertNotNull(user);
        assertEquals("user123", user.getUserId());
    }

    @Test
    void getUserByUserIdWhenNotExistingThrowsException() {
        when(userRepository.findUserByUserId("nonexistent")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> userService.getUserByUserId("nonexistent"));

        assertEquals("User by user id not found", exception.getMessage());
    }

    @Test
    void lockedUserSetsAccountNonLocked() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));

        userService.lockedUser("test@example.com");

        assertFalse(userEntity.isAccountNonLocked());
        verify(userRepository).save(userEntity);
    }

    @Test
    void verifyMfaWithInvalidCode() {
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(userEntity));
        when(mfaService.validateOtp("secretKey", 123456)).thenReturn(false);
        userEntity.setQrCodeSecret("secretKey");

        boolean result = userService.verifyMfa("test@example.com", 123456);

        assertFalse(result);
    }

    @Test
    void getRoleNameReturnsRoleWhenExists() {
        when(roleRepository.findByNameIgnoreCase("USER")).thenReturn(Optional.of(roleEntity));

        RoleEntity role = userService.getRoleName("USER");

        assertEquals(roleEntity, role);
    }

    @Test
    void getRoleNameThrowsExceptionWhenRoleNotFound() {
        when(roleRepository.findByNameIgnoreCase("NONEXISTENT")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> userService.getRoleName("NONEXISTENT"));

        assertEquals("Role not found", exception.getMessage());
    }

}
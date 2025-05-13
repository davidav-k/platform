package com.example.user_service.service;

import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.dto.User;
import com.example.user_service.dto.UserRequest;
import com.example.user_service.entity.CredentialEntity;
import com.example.user_service.entity.RoleEntity;
import com.example.user_service.entity.UserEntity;
import com.example.user_service.enumeration.LoginType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

public interface UserService {

    void createUser(String firstName, String lastName, String email, String password);

    RoleEntity getRoleName(String name);

    void verifyAccountKey(String key);

    User getUserByUserId(String apply);

    User getUserByEmail(String email);

    UserEntity getUserEntityByEmail(String email);

    CredentialEntity getUserCredentialById(Long id);

    ApiAuthentication authenticateUser(String email, String password, HttpServletRequest request);

    void enableMfa(String email);

    boolean verifyMfa(String email, int code);

    void logLoginAttempt(UserEntity user, boolean success, String ip, String userAgent);

    void updateLoginAttempt(String email, LoginType loginType, HttpServletRequest request);

    void unlockedUser(String email);

    void updateUser(Long userId, @Valid UserRequest userRequest);

    void changePassword(Long id, String oldPassword, String newPassword, String confirmNewPassword);

    void deleteUser(Long userId, Authentication authentication);

    void lockedUser(String email);
}


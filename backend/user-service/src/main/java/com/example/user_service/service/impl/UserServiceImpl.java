package com.example.user_service.service.impl;

import com.example.user_service.cache.CacheStore;
import com.example.user_service.domain.ApiAuthentication;
import com.example.user_service.domain.RequestContext;
import com.example.user_service.dto.User;
import com.example.user_service.dto.UserRequest;
import com.example.user_service.entity.*;
import com.example.user_service.enumeration.Authority;
import com.example.user_service.enumeration.EventType;
import com.example.user_service.enumeration.LoginType;
import com.example.user_service.event.UserEvent;
import com.example.user_service.exception.ApiException;
import com.example.user_service.repository.*;
import com.example.user_service.service.MfaService;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;


@Service
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialRepository credentialRepository;
    private final ConfirmationRepository confirmationRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ApplicationEventPublisher publisher;
    private final CacheStore<String, Integer> userCache;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;

    @Override
    public void createUser(String firstName, String lastName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("User with this email already exists");
        }
        UserEntity userEntity = userRepository.save(createNewUser(firstName, lastName, email));
        CredentialEntity credentialEntity = new CredentialEntity(userEntity, passwordEncoder.encode(password));
        credentialRepository.save(credentialEntity);
        ConfirmationEntity confirmationEntity = new ConfirmationEntity(userEntity);
        confirmationRepository.save(confirmationEntity);
        publisher.publishEvent(new UserEvent(userEntity, EventType.REGISTRATION, Map.of("key", confirmationEntity.getKey())));
    }

    private UserEntity createNewUser(String firstName, String lastName, String email) {
        RoleEntity role = getRoleName(Authority.USER.name());
        return UserUtils.createUserEntity(firstName, lastName, email, role);
    }

    @Override
    public RoleEntity getRoleName(String name) {
        return roleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ApiException("Role not found"));
    }

    @Override
    public void verifyAccountKey(String key) {
        ConfirmationEntity confirmationEntity = getUserConfirmation(key);
        UserEntity userEntity = getUserEntityByEmail(confirmationEntity.getUserEntity().getEmail());
        RequestContext.setUserId(userEntity.getId());
        userEntity.setEnabled(true);
        userRepository.save(userEntity);
        confirmationRepository.delete(confirmationEntity);
    }

    private ConfirmationEntity getUserConfirmation(String key) {
        return confirmationRepository.findByKey(key)
                .orElseThrow(() -> new ApiException("Invalid key"));
    }

    @Override
    public UserEntity getUserEntityByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException("User by email not found"));
    }

    @Override
    public User getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findUserByUserId(userId).orElseThrow(() -> new ApiException("User by user id not found"));
        return UserUtils.fromUserEntity(userEntity, userEntity.getRole(),getUserCredentialById(userEntity.getId()));
    }

    @Override
    public User getUserByEmail(String email) {
        UserEntity userEntity = getUserEntityByEmail(email);
        return UserUtils.fromUserEntity(userEntity, userEntity.getRole(),getUserCredentialById(userEntity.getId()));
    }

    @Override
    public CredentialEntity getUserCredentialById(Long userId) {
        return credentialRepository.getCredentialByUserEntityId(userId)
                .orElseThrow(() -> new ApiException("Unable to find user credential"));
    }

    @Override
    public ApiAuthentication authenticateUser(String email, String password, HttpServletRequest request) {
        UserEntity userEntity = getUserEntityByEmail(email);
        CredentialEntity credential = credentialRepository.getCredentialByUserEntityId(userEntity.getId())
                .orElseThrow(() -> new ApiException("Invalid credentials"));
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        if (!passwordEncoder.matches(password, credential.getPassword())) {
            logLoginAttempt(userEntity, false, ip, userAgent);
            throw new ApiException("Invalid email or password");
        }

        logLoginAttempt(userEntity, true, ip, userAgent);

        return ApiAuthentication.authenticated(
                UserUtils.fromUserEntity(userEntity, userEntity.getRole(), credential),
                AuthorityUtils.commaSeparatedStringToAuthorityList(userEntity.getRole().getAuthorities().getValue())
        );
    }

    @Override
    public void enableMfa(String email) {
        UserEntity user = getUserEntityByEmail(email);
        String secretKey = mfaService.generateSecretKey();
        String qrCodeUrl = mfaService.generateQrCodeUrl(user.getEmail(), secretKey);

        user.setMfa(true);
        user.setQrCodeSecret(secretKey);
        user.setQrCodeImageUrl(qrCodeUrl);

        userRepository.save(user);
    }

    @Override
    public boolean verifyMfa(String email, int code) {
        UserEntity user = getUserEntityByEmail(email);
        return mfaService.validateOtp(user.getQrCodeSecret(), code);
    }

    @Override
    public void logLoginAttempt(UserEntity user, boolean success, String ip, String userAgent) {
        LoginHistoryEntity log = LoginHistoryEntity.builder()
                .user(user)
                .loginTime(LocalDateTime.now())
                .ip(ip)
                .userAgent(userAgent)
                .success(success)
                .build();
        loginHistoryRepository.save(log);
    }

    @Override
    public void updateLoginAttempt(String email, LoginType loginType, HttpServletRequest request) {
        UserEntity userEntity = getUserEntityByEmail(email);
        RequestContext.setUserId(userEntity.getId());
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        switch (loginType) {
            case LOGIN_ATTEMPT -> {
                userEntity.setLoginAttempts(userEntity.getLoginAttempts() + 1);
                userCache.put(userEntity.getEmail(), userEntity.getLoginAttempts());
                if (userCache.get(userEntity.getEmail()) > 5) {
                    userEntity.setAccountNonLocked(false);
                }

            }
            case LOGIN_SUCCESS -> {
                userEntity.setAccountNonLocked(true);
                userEntity.setLoginAttempts(0);
                userEntity.setLastLogin(LocalDate.now());
                userCache.evict(userEntity.getEmail());
                logLoginAttempt(userEntity, true, ip, userAgent);
            }
        }
        userRepository.save(userEntity);
    }

    @Override
    public void unlockedUser(String email) {
        UserEntity userEntity = getUserEntityByEmail(email);
        userCache.evict(userEntity.getEmail());
        userEntity.setLoginAttempts(0);
        userEntity.setAccountNonLocked(true);
        userRepository.save(userEntity);
    }

    @Override
    public void updateUser(Long userId, UserRequest userRequest) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            if (!userEntity.getEmail().equals(userRequest.getEmail())) {
                throw new ApiException("User with this email already exists");
            }
        }
        userEntity.setFirstName(userRequest.getFirstName());
        userEntity.setLastName(userRequest.getLastName());
        userEntity.setPhone(userRequest.getPhone());
        userEntity.setBio(userRequest.getBio());
        userRepository.save(userEntity);
    }

    @Override
    public void lockedUser(String email) {
        UserEntity userEntity = getUserEntityByEmail(email);
        userEntity.setAccountNonLocked(false);
        userRepository.save(userEntity);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword, String confirmNewPassword) {

        CredentialEntity credentialEntity = getUserCredentialById(id);
        if (!passwordEncoder.matches(oldPassword, credentialEntity.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new ApiException("New password and confirm new password do not match");
        }
        //The new password must contain at least one digit, one lowercase letter, one uppercase letter, and be at least 8 characters long.
        String passwordPolicy = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (!newPassword.matches(passwordPolicy)) {
            throw new ApiException("New password does not conform to password policy");
        }
        credentialEntity.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    public void deleteUser(Long userId, Authentication authentication) {

    }


}


package com.example.user_service.outbox;

import com.example.user_service.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserOutboxPayloadFactory {

    private final ObjectMapper objectMapper;

    public String userRegisteredPayload(UserEntity user) {
        return serialize(new UserRegisteredPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                user.getFirstName(),
                user.getLastName(),
                now()
        ), UserOutboxEventTypes.USER_REGISTERED);
    }

    public String userLoginSuccessPayload(UserEntity user) {
        return serialize(new UserLoginSuccessPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                "SUCCESS",
                now()
        ), UserOutboxEventTypes.USER_LOGIN_SUCCESS);
    }

    public String userLoginFailedPayload(String userId, String email, String role, String failureReason) {
        return serialize(new UserLoginFailedPayload(
                userId,
                email,
                role,
                "FAILED",
                failureReason,
                now()
        ), UserOutboxEventTypes.USER_LOGIN_FAILED);
    }

    public String userProfileUpdatedPayload(UserEntity user, List<String> changedFields) {
        return serialize(new UserProfileUpdatedPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                user.getFirstName(),
                user.getLastName(),
                changedFields,
                now()
        ), UserOutboxEventTypes.USER_PROFILE_UPDATED);
    }

    public String userDeletedPayload(UserEntity user, String deletedByUserId) {
        return serialize(new UserDeletedPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                user.getFirstName(),
                user.getLastName(),
                deletedByUserId,
                now()
        ), UserOutboxEventTypes.USER_DELETED);
    }

    public String passwordChangedPayload(UserEntity user) {
        return serialize(new PasswordChangedPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                now()
        ), UserOutboxEventTypes.PASSWORD_CHANGED);
    }

    public String mfaEnabledPayload(UserEntity user) {
        return serialize(new MfaEnabledPayload(
                user.getUserId(),
                user.getEmail(),
                role(user),
                now()
        ), UserOutboxEventTypes.MFA_ENABLED);
    }

    private String serialize(Object payload, String eventType) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize " + eventType + " payload", exception);
        }
    }

    private String role(UserEntity user) {
        return user.getRole() == null ? null : user.getRole().getName();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record UserRegisteredPayload(
            String userId,
            String email,
            String role,
            String firstName,
            String lastName,
            OffsetDateTime registeredAt
    ) {
    }

    private record UserLoginSuccessPayload(
            String userId,
            String email,
            String role,
            String result,
            OffsetDateTime loggedInAt
    ) {
    }

    private record UserLoginFailedPayload(
            String userId,
            String email,
            String role,
            String result,
            String failureReason,
            OffsetDateTime attemptedAt
    ) {
    }

    private record UserProfileUpdatedPayload(
            String userId,
            String email,
            String role,
            String firstName,
            String lastName,
            List<String> changedFields,
            OffsetDateTime updatedAt
    ) {
    }

    private record UserDeletedPayload(
            String userId,
            String email,
            String role,
            String firstName,
            String lastName,
            String deletedByUserId,
            OffsetDateTime deletedAt
    ) {
    }

    private record PasswordChangedPayload(
            String userId,
            String email,
            String role,
            OffsetDateTime changedAt
    ) {
    }

    private record MfaEnabledPayload(
            String userId,
            String email,
            String role,
            OffsetDateTime enabledAt
    ) {
    }
}

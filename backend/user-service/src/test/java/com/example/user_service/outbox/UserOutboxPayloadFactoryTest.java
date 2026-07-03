package com.example.user_service.outbox;

import com.example.user_service.entity.RoleEntity;
import com.example.user_service.entity.UserEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserOutboxPayloadFactoryTest {

    private ObjectMapper objectMapper;
    private UserOutboxPayloadFactory payloadFactory;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        payloadFactory = new UserOutboxPayloadFactory(objectMapper);

        RoleEntity role = new RoleEntity();
        role.setName("USER");

        user = new UserEntity();
        user.setUserId("10000000-0000-0000-0000-000000000001");
        user.setEmail("audit.user@example.com");
        user.setFirstName("Audit");
        user.setLastName("User");
        user.setRole(role);
        user.setQrCodeSecret("mfa-secret-must-not-leak");
        user.setQrCodeImageUrl("otpauth://secret-must-not-leak");
    }

    @Test
    void serializesSupportedUserAuditPayloadsWithoutSensitiveFields() throws Exception {
        List<String> payloads = List.of(
                payloadFactory.userRegisteredPayload(user),
                payloadFactory.userLoginSuccessPayload(user),
                payloadFactory.userLoginFailedPayload(
                        user.getUserId(), user.getEmail(), "USER", "BAD_CREDENTIALS"),
                payloadFactory.userProfileUpdatedPayload(user, List.of("firstName")),
                payloadFactory.userDeletedPayload(user, "20000000-0000-0000-0000-000000000002"),
                payloadFactory.passwordChangedPayload(user),
                payloadFactory.mfaEnabledPayload(user)
        );

        for (String payload : payloads) {
            JsonNode json = objectMapper.readTree(payload);
            assertThat(json.get("email").asText()).isEqualTo("audit.user@example.com");
            assertThat(payload)
                    .doesNotContainIgnoringCase("password")
                    .doesNotContainIgnoringCase("token")
                    .doesNotContainIgnoringCase("secret")
                    .doesNotContainIgnoringCase("qrCode")
                    .doesNotContainIgnoringCase("confirmation")
                    .doesNotContain("mfa-secret-must-not-leak");
        }
    }

    @Test
    void failedLoginPayloadContainsOnlySafeResultCategory() throws Exception {
        JsonNode payload = objectMapper.readTree(payloadFactory.userLoginFailedPayload(
                null,
                "unknown@example.com",
                null,
                "AUTHENTICATION_FAILED"
        ));

        assertThat(payload.get("userId").isNull()).isTrue();
        assertThat(payload.get("email").asText()).isEqualTo("unknown@example.com");
        assertThat(payload.get("result").asText()).isEqualTo("FAILED");
        assertThat(payload.get("failureReason").asText()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(payload.get("attemptedAt").asText()).isNotBlank();
    }
}

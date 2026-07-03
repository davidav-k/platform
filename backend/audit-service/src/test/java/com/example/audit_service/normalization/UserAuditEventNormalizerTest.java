package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class UserAuditEventNormalizerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AGGREGATE_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-07-03T08:30:00Z");

    private ObjectMapper objectMapper;
    private UserAuditEventNormalizer normalizer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        normalizer = new UserAuditEventNormalizer(objectMapper);
    }

    @Test
    void normalizesUserRegistered() {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                "USER_REGISTERED",
                """
                    {"userId":"%s","email":"user@example.com","role":"USER"}
                    """.formatted(USER_ID)
        )).orElseThrow();

        assertThat(normalizedEvent.eventId()).isEqualTo(EVENT_ID);
        assertThat(normalizedEvent.aggregateId()).isEqualTo(AGGREGATE_ID);
        assertThat(normalizedEvent.sourceService()).isEqualTo("user-service");
        assertThat(normalizedEvent.actorUserId()).isEqualTo(USER_ID);
        assertThat(normalizedEvent.actorEmail()).isEqualTo("user@example.com");
        assertThat(normalizedEvent.action()).isEqualTo("REGISTER_USER");
        assertThat(normalizedEvent.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @ParameterizedTest
    @CsvSource({
            "USER_LOGIN_SUCCESS, LOGIN_SUCCESS",
            "USER_PROFILE_UPDATED, UPDATE_USER_PROFILE",
            "USER_DELETED, DELETE_USER",
            "PASSWORD_CHANGED, CHANGE_PASSWORD",
            "MFA_ENABLED, ENABLE_MFA"
    })
    void normalizesOtherPublishedUserEvents(String eventType, String action) {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                eventType,
                """
                    {"userId":"%s","email":"user@example.com"}
                    """.formatted(USER_ID)
        )).orElseThrow();

        assertThat(normalizedEvent.action()).isEqualTo(action);
    }

    @Test
    void normalizesUserLoginFailedWithoutKnownUser() {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                "USER_LOGIN_FAILED",
                """
                    {"userId":null,"email":"unknown@example.com","result":"FAILED"}
                    """
        )).orElseThrow();

        assertThat(normalizedEvent.actorUserId()).isNull();
        assertThat(normalizedEvent.actorEmail()).isEqualTo("unknown@example.com");
        assertThat(normalizedEvent.action()).isEqualTo("LOGIN_FAILED");
    }

    @Test
    void removesSensitiveFieldsRecursively() throws Exception {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                "USER_REGISTERED",
                """
                    {
                      "userId":"%s",
                      "email":"user@example.com",
                      "password":"password-value",
                      "access_token":"access-token-value",
                      "refreshToken":"refresh-token-value",
                      "jwt":"jwt-value",
                      "mfaSecret":"mfa-secret-value",
                      "qrCodeSecret":"qr-secret-value",
                      "confirmationKey":"confirmation-value",
                      "nested":{"resetToken":"reset-value"},
                      "items":[{"newPassword":"new-password-value"}]
                    }
                    """.formatted(USER_ID)
        )).orElseThrow();

        JsonNode payload = objectMapper.readTree(normalizedEvent.payload());
        assertThat(payload.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(payload.get("email").asText()).isEqualTo("user@example.com");
        assertThat(normalizedEvent.payload())
                .doesNotContain("password-value")
                .doesNotContain("access-token-value")
                .doesNotContain("refresh-token-value")
                .doesNotContain("jwt-value")
                .doesNotContain("mfa-secret-value")
                .doesNotContain("qr-secret-value")
                .doesNotContain("confirmation-value")
                .doesNotContain("reset-value")
                .doesNotContain("new-password-value");
    }

    @Test
    void logsAndIgnoresUnsupportedUserEventType(CapturedOutput output) {
        Optional<NormalizedAuditEvent> normalizedEvent =
                normalizer.normalize(event("USER_ROLE_CHANGED", "{}"));

        assertThat(normalizedEvent).isEmpty();
        assertThat(output)
                .contains("Ignoring unsupported user audit event")
                .contains("USER_ROLE_CHANGED")
                .contains(EVENT_ID.toString());
    }

    private KafkaOutboxEventMessage event(String eventType, String payload) {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                eventType,
                "USER",
                AGGREGATE_ID,
                OCCURRED_AT,
                1,
                payload
        );
    }
}

package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificationAuditEventNormalizerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID NOTIFICATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID RECIPIENT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID SOURCE_ENTITY_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final OffsetDateTime OCCURRED_AT =
            OffsetDateTime.parse("2026-07-03T08:30:00Z");

    private ObjectMapper objectMapper;
    private NotificationAuditEventNormalizer normalizer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        normalizer = new NotificationAuditEventNormalizer(objectMapper);
    }

    @Test
    void normalizesNotificationCreated() {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                "NOTIFICATION_CREATED",
                payload(null, null, null)
        )).orElseThrow();

        assertThat(normalizedEvent.eventId()).isEqualTo(EVENT_ID);
        assertThat(normalizedEvent.eventType()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(normalizedEvent.aggregateType()).isEqualTo("NOTIFICATION");
        assertThat(normalizedEvent.aggregateId()).isEqualTo(NOTIFICATION_ID);
        assertThat(normalizedEvent.sourceService()).isEqualTo("notification-service");
        assertThat(normalizedEvent.actorUserId()).isNull();
        assertThat(normalizedEvent.actorEmail()).isNull();
        assertThat(normalizedEvent.action()).isEqualTo("CREATE_NOTIFICATION");
        assertThat(normalizedEvent.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void normalizesNotificationSystemCreated() throws Exception {
        NormalizedAuditEvent normalizedEvent = normalizer.normalize(event(
                "NOTIFICATION_SYSTEM_CREATED",
                payload("task-service", "TASK", SOURCE_ENTITY_ID)
        )).orElseThrow();

        assertThat(normalizedEvent.action()).isEqualTo("CREATE_SYSTEM_NOTIFICATION");
        JsonNode payload = objectMapper.readTree(normalizedEvent.payload());
        assertThat(payload.get("recipientUserId").asText()).isEqualTo(RECIPIENT_ID.toString());
        assertThat(payload.get("sourceService").asText()).isEqualTo("task-service");
        assertThat(payload.get("sourceEntityType").asText()).isEqualTo("TASK");
        assertThat(payload.get("sourceEntityId").asText()).isEqualTo(SOURCE_ENTITY_ID.toString());
    }

    @Test
    void preservesBusinessBodyAndRemovesSensitiveTechnicalFieldsRecursively() {
        String payload = """
                {
                  "notificationId":"%s",
                  "recipientUserId":"%s",
                  "body":"Visible notification business data",
                  "jwt":"jwt-value",
                  "authorizationHeader":"Bearer secret-value",
                  "requestHeaders":{"X-Internal-Secret":"header-value"},
                  "metadata":[{"cookie":"cookie-value"},{"accessToken":"token-value"}]
                }
                """.formatted(NOTIFICATION_ID, RECIPIENT_ID);

        NormalizedAuditEvent normalizedEvent = normalizer.normalize(
                event("NOTIFICATION_CREATED", payload)
        ).orElseThrow();

        assertThat(normalizedEvent.payload())
                .contains("Visible notification business data")
                .doesNotContain("jwt-value")
                .doesNotContain("secret-value")
                .doesNotContain("header-value")
                .doesNotContain("cookie-value")
                .doesNotContain("token-value");
    }

    @Test
    void logsAndIgnoresUnsupportedNotificationEvent(CapturedOutput output) {
        Optional<NormalizedAuditEvent> normalizedEvent =
                normalizer.normalize(event("NOTIFICATION_SENT", "{}"));

        assertThat(normalizedEvent).isEmpty();
        assertThat(output)
                .contains("Ignoring unsupported notification audit event")
                .contains("NOTIFICATION_SENT")
                .contains(EVENT_ID.toString());
    }

    private KafkaOutboxEventMessage event(String eventType, String payload) {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                eventType,
                "NOTIFICATION",
                NOTIFICATION_ID,
                OCCURRED_AT,
                1,
                payload
        );
    }

    private String payload(String sourceService, String sourceEntityType, UUID sourceEntityId) {
        return """
                {
                  "notificationId":"%s",
                  "recipientUserId":"%s",
                  "type":"TASK_ASSIGNED",
                  "channel":"IN_APP",
                  "status":"PENDING",
                  "sourceService":%s,
                  "sourceEntityType":%s,
                  "sourceEntityId":%s,
                  "createdAt":"2026-07-03T08:30:00Z",
                  "updatedAt":"2026-07-03T08:30:00Z",
                  "sentAt":null
                }
                """.formatted(
                NOTIFICATION_ID,
                RECIPIENT_ID,
                jsonString(sourceService),
                jsonString(sourceEntityType),
                sourceEntityId == null ? "null" : "\"" + sourceEntityId + "\""
        );
    }

    private String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}

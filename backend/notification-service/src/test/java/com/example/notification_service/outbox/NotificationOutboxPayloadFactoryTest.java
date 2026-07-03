package com.example.notification_service.outbox;

import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationOutboxPayloadFactoryTest {

    private static final UUID NOTIFICATION_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RECIPIENT_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SOURCE_ENTITY_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.parse("2026-07-03T08:30:00Z");

    private ObjectMapper objectMapper;
    private NotificationOutboxPayloadFactory payloadFactory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        payloadFactory = new NotificationOutboxPayloadFactory(objectMapper);
    }

    @Test
    void createsSafeNotificationPayloadWithExpectedMetadata() throws Exception {
        NotificationEntity notification = notification();

        JsonNode payload = objectMapper.readTree(
                payloadFactory.notificationSystemCreatedPayload(notification)
        );

        assertThat(payload.get("notificationId").asText()).isEqualTo(NOTIFICATION_ID.toString());
        assertThat(payload.get("recipientUserId").asText()).isEqualTo(RECIPIENT_ID.toString());
        assertThat(payload.get("type").asText()).isEqualTo("TASK_ASSIGNED");
        assertThat(payload.get("channel").asText()).isEqualTo("IN_APP");
        assertThat(payload.get("status").asText()).isEqualTo("PENDING");
        assertThat(payload.get("sourceService").asText()).isEqualTo("task-service");
        assertThat(payload.get("sourceEntityType").asText()).isEqualTo("TASK");
        assertThat(payload.get("sourceEntityId").asText()).isEqualTo(SOURCE_ENTITY_ID.toString());
        assertThat(payload.get("createdAt").asText()).isEqualTo("2026-07-03T08:30:00Z");
        assertThat(payload.get("updatedAt").asText()).isEqualTo("2026-07-03T08:30:00Z");
        assertThat(payload.get("sentAt").isNull()).isTrue();
        assertThat(payload.has("subject")).isFalse();
        assertThat(payload.has("body")).isFalse();
        assertThat(payload.toString())
                .doesNotContain("access-token-value")
                .doesNotContain("cookie-value")
                .doesNotContain("authorization-header-value");
    }

    private NotificationEntity notification() {
        NotificationEntity notification = new NotificationEntity(
                NOTIFICATION_ID,
                RECIPIENT_ID,
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.IN_APP,
                "access-token-value",
                "cookie-value authorization-header-value",
                NotificationStatus.PENDING,
                "task-service",
                "TASK",
                SOURCE_ENTITY_ID
        );
        ReflectionTestUtils.setField(notification, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(notification, "updatedAt", CREATED_AT);
        return notification;
    }
}

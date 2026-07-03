package com.example.notification_service.outbox;

import com.example.notification_service.entity.NotificationEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationOutboxPayloadFactory {

    private final ObjectMapper objectMapper;

    public String notificationCreatedPayload(NotificationEntity notification) {
        return serialize(notification, NotificationOutboxEventTypes.NOTIFICATION_CREATED);
    }

    public String notificationSystemCreatedPayload(NotificationEntity notification) {
        return serialize(notification, NotificationOutboxEventTypes.NOTIFICATION_SYSTEM_CREATED);
    }

    private String serialize(NotificationEntity notification, String eventType) {
        NotificationAuditPayload payload = new NotificationAuditPayload(
                notification.getNotificationId(),
                notification.getRecipientUserId(),
                notification.getType().name(),
                notification.getChannel().name(),
                notification.getStatus().name(),
                notification.getSourceService(),
                notification.getSourceEntityType(),
                notification.getSourceEntityId(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getSentAt()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize " + eventType + " payload", exception);
        }
    }

    private record NotificationAuditPayload(
            UUID notificationId,
            UUID recipientUserId,
            String type,
            String channel,
            String status,
            String sourceService,
            String sourceEntityType,
            UUID sourceEntityId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime sentAt
    ) {
    }
}

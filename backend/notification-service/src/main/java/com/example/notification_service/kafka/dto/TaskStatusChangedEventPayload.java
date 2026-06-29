package com.example.notification_service.kafka.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskStatusChangedEventPayload(
    UUID taskId,
    String title,
    String previousStatus,
    String newStatus,
    String priority,
    UUID assigneeUserId,
    UUID createdByUserId,
    OffsetDateTime updatedAt
) {
}

package com.example.notification_service.kafka.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskCreatedEventPayload(
    UUID taskId,
    String title,
    String description,
    String status,
    String priority,
    UUID assigneeUserId,
    UUID createdByUserId,
    OffsetDateTime createdAt
) {
}

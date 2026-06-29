package com.example.notification_service.kafka.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskAssignedEventPayload(
    UUID taskId,
    String title,
    String status,
    String priority,
    UUID previousAssigneeUserId,
    UUID newAssigneeUserId,
    UUID createdByUserId,
    OffsetDateTime updatedAt
) {
}

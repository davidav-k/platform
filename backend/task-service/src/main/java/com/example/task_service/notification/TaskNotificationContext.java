package com.example.task_service.notification;

import java.util.UUID;

public record TaskNotificationContext(
    UUID taskId,
    String title,
    UUID assigneeUserId,
    UUID createdByUserId
) {
}

package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTaskResponse(
        UUID taskId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        OffsetDateTime createdAt) {
}

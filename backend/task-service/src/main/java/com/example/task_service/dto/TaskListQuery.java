package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;

import java.util.UUID;

public record TaskListQuery(
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        int page,
        int size,
        String sort) {
}

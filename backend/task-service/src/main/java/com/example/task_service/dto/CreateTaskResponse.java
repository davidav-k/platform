package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CreateTaskResponse {

    private final UUID taskId;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final TaskPriority priority;
    private final UUID assigneeUserId;
    private final UUID createdByUserId;
    private final OffsetDateTime createdAt;

    public CreateTaskResponse(UUID taskId, String title, String description, TaskStatus status,
                              TaskPriority priority, UUID assigneeUserId, UUID createdByUserId,
                              OffsetDateTime createdAt) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assigneeUserId = assigneeUserId;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

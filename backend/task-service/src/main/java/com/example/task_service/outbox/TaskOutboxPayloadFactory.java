package com.example.task_service.outbox;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskOutboxPayloadFactory {

    private final ObjectMapper objectMapper;

    public String taskCreatedPayload(TaskEntity task) {
        try {
            return objectMapper.writeValueAsString(new TaskCreatedPayload(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                task.getCreatedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TASK_CREATED payload", exception);
        }
    }

    public String taskAssignedPayload(TaskEntity task, UUID previousAssigneeUserId) {
        try {
            return objectMapper.writeValueAsString(new TaskAssignedPayload(
                task.getTaskId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                previousAssigneeUserId,
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                task.getUpdatedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TASK_ASSIGNED payload", exception);
        }
    }

    public String taskStatusChangedPayload(TaskEntity task, TaskStatus previousStatus) {
        try {
            return objectMapper.writeValueAsString(new TaskStatusChangedPayload(
                task.getTaskId(),
                task.getTitle(),
                previousStatus,
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                task.getUpdatedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TASK_STATUS_CHANGED payload", exception);
        }
    }

    public String taskUpdatedPayload(TaskEntity task, List<String> changedFields, UUID actorUserId) {
        try {
            return objectMapper.writeValueAsString(new TaskUpdatedPayload(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                actorUserId,
                task.getUpdatedAt(),
                changedFields
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TASK_UPDATED payload", exception);
        }
    }

    public String taskDeletedPayload(TaskEntity task) {
        try {
            return objectMapper.writeValueAsString(new TaskDeletedPayload(
                task.getTaskId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                task.getDeletedByUserId(),
                task.getDeletedAt()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize TASK_DELETED payload", exception);
        }
    }

    private record TaskCreatedPayload(
        UUID taskId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        OffsetDateTime createdAt
    ) {
    }

    private record TaskAssignedPayload(
        UUID taskId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        UUID previousAssigneeUserId,
        UUID newAssigneeUserId,
        UUID createdByUserId,
        OffsetDateTime updatedAt
    ) {
    }

    private record TaskStatusChangedPayload(
        UUID taskId,
        String title,
        TaskStatus previousStatus,
        TaskStatus newStatus,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        OffsetDateTime updatedAt
    ) {
    }

    private record TaskUpdatedPayload(
        UUID taskId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        UUID actorUserId,
        OffsetDateTime updatedAt,
        List<String> changedFields
    ) {
    }

    private record TaskDeletedPayload(
        UUID taskId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeUserId,
        UUID createdByUserId,
        UUID deletedByUserId,
        OffsetDateTime deletedAt
    ) {
    }
}

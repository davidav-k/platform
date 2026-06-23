package com.example.task_service.outbox;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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
}

package com.example.task_service.usecase.impl;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskStatusRequest;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.outbox.OutboxEventService;
import com.example.task_service.outbox.TaskOutboxPayloadFactory;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.ChangeTaskStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeTaskStatusUseCaseImpl implements ChangeTaskStatusUseCase {

    private static final String TASK_AGGREGATE_TYPE = "TASK";
    private static final String TASK_STATUS_CHANGED_EVENT_TYPE = "TASK_STATUS_CHANGED";

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;
    private final OutboxEventService outboxEventService;
    private final TaskOutboxPayloadFactory taskOutboxPayloadFactory;

    @Override
    public TaskResponse changeStatus(UUID taskId, UpdateTaskStatusRequest request) {
        validate(taskId, request);

        TaskEntity task = taskRepository.findByTaskIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.canAccess(task.getCreatedByUserId(), task.getAssigneeUserId())) {
            throw new TaskNotFoundException(taskId);
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(request.status());
        TaskEntity updatedTask = taskRepository.saveAndFlush(task);
        saveTaskStatusChangedOutboxEvent(updatedTask, previousStatus);
        return toResponse(updatedTask);
    }

    private void saveTaskStatusChangedOutboxEvent(TaskEntity task, TaskStatus previousStatus) {
        outboxEventService.saveNewEvent(
            TASK_AGGREGATE_TYPE,
            task.getTaskId(),
            TASK_STATUS_CHANGED_EVENT_TYPE,
            taskOutboxPayloadFactory.taskStatusChangedPayload(task, previousStatus)
        );
    }

    private void validate(UUID taskId, UpdateTaskStatusRequest request) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Task status request is required");
        }
        if (request.status() == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }

    private TaskResponse toResponse(TaskEntity task) {
        return new TaskResponse(
            task.getTaskId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getAssigneeUserId(),
            task.getCreatedByUserId(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}

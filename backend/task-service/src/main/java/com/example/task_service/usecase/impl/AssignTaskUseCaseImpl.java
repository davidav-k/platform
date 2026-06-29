package com.example.task_service.usecase.impl;

import com.example.task_service.dto.AssignTaskRequest;
import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.outbox.OutboxEventService;
import com.example.task_service.outbox.TaskOutboxPayloadFactory;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.AssignTaskUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignTaskUseCaseImpl implements AssignTaskUseCase {

    private static final String TASK_AGGREGATE_TYPE = "TASK";
    private static final String TASK_ASSIGNED_EVENT_TYPE = "TASK_ASSIGNED";

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;
    private final OutboxEventService outboxEventService;
    private final TaskOutboxPayloadFactory taskOutboxPayloadFactory;

    @Override
    public TaskResponse assign(UUID taskId, AssignTaskRequest request) {
        validate(taskId, request);

        TaskEntity task = taskRepository.findByTaskIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.canManage(task.getCreatedByUserId())) {
            throw new TaskNotFoundException(taskId);
        }

        UUID previousAssigneeUserId = task.getAssigneeUserId();
        task.setAssigneeUserId(request.getAssigneeUserId());
        TaskEntity updatedTask = taskRepository.saveAndFlush(task);
        saveTaskAssignedOutboxEvent(updatedTask, previousAssigneeUserId);
        return toResponse(updatedTask);
    }

    private void saveTaskAssignedOutboxEvent(TaskEntity task, UUID previousAssigneeUserId) {
        outboxEventService.saveNewEvent(
            TASK_AGGREGATE_TYPE,
            task.getTaskId(),
            TASK_ASSIGNED_EVENT_TYPE,
            taskOutboxPayloadFactory.taskAssignedPayload(task, previousAssigneeUserId)
        );
    }

    private void validate(UUID taskId, AssignTaskRequest request) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Task assignment request is required");
        }
        if (!request.isAssigneeUserIdPresent()) {
            throw new IllegalArgumentException("Assignee user ID must be provided");
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

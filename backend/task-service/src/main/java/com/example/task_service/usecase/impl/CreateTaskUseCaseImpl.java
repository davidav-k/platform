package com.example.task_service.usecase.impl;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.outbox.OutboxEventService;
import com.example.task_service.outbox.TaskOutboxPayloadFactory;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.usecase.CreateTaskUseCase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateTaskUseCaseImpl implements CreateTaskUseCase {

    private static final String TASK_AGGREGATE_TYPE = "TASK";
    private static final String TASK_CREATED_EVENT_TYPE = "TASK_CREATED";

    private final TaskRepository taskRepository;
    private final Validator validator;
    private final OutboxEventService outboxEventService;
    private final TaskOutboxPayloadFactory taskOutboxPayloadFactory;

    @Override
    public CreateTaskResponse create(CreateTaskRequest request, UUID createdByUserId) {
        validate(request, createdByUserId);

        TaskEntity task = new TaskEntity(
                UUID.randomUUID(),
                request.getTitle().trim(),
                request.getDescription(),
                TaskStatus.NEW,
                priorityOrDefault(request),
                request.getAssigneeUserId(),
                createdByUserId
        );

        TaskEntity saved = taskRepository.saveAndFlush(task);
        saveTaskCreatedOutboxEvent(saved);
        return toResponse(saved);
    }

    private void saveTaskCreatedOutboxEvent(TaskEntity task) {
        outboxEventService.saveNewEvent(
            TASK_AGGREGATE_TYPE,
            task.getTaskId(),
            TASK_CREATED_EVENT_TYPE,
            taskOutboxPayloadFactory.taskCreatedPayload(task)
        );
    }

    private void validate(CreateTaskRequest request, UUID createdByUserId) {
        if (request == null) {
            throw new IllegalArgumentException("Create task request is required");
        }
        if (createdByUserId == null) {
            throw new IllegalArgumentException("Creator user ID is required");
        }
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private TaskPriority priorityOrDefault(CreateTaskRequest request) {
        return request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority();
    }

    private CreateTaskResponse toResponse(TaskEntity task) {
        return new CreateTaskResponse(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId(),
                task.getCreatedAt()
        );
    }
}

package com.example.task_service.usecase.impl;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;
import com.example.task_service.notification.TaskNotificationContext;
import com.example.task_service.notification.TaskNotificationPublisher;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.usecase.CreateTaskUseCase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateTaskUseCaseImpl implements CreateTaskUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTaskUseCaseImpl.class);

    private final TaskRepository taskRepository;
    private final Validator validator;
    private final TaskNotificationPublisher taskNotificationPublisher;

    public CreateTaskUseCaseImpl(TaskRepository taskRepository, Validator validator,
                                 TaskNotificationPublisher taskNotificationPublisher) {
        this.taskRepository = taskRepository;
        this.validator = validator;
        this.taskNotificationPublisher = taskNotificationPublisher;
    }

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
        CreateTaskResponse response = toResponse(saved);
        publishAssignmentNotification(response);
        return response;
    }

    private void publishAssignmentNotification(CreateTaskResponse task) {
        if (task.getAssigneeUserId() == null) {
            return;
        }
        try {
            taskNotificationPublisher.notifyTaskAssigned(new TaskNotificationContext(
                task.getTaskId(),
                task.getTitle(),
                task.getAssigneeUserId(),
                task.getCreatedByUserId()
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("Task assignment notification failed for taskId={} and assigneeUserId={}",
                task.getTaskId(), task.getAssigneeUserId(), exception);
        }
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

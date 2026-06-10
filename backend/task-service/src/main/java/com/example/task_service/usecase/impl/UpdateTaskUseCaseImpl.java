package com.example.task_service.usecase.impl;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskRequest;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.UpdateTaskUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateTaskUseCaseImpl implements UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;

    public UpdateTaskUseCaseImpl(TaskRepository taskRepository,
                                 CurrentUserAccessProvider currentUserAccessProvider) {
        this.taskRepository = taskRepository;
        this.currentUserAccessProvider = currentUserAccessProvider;
    }

    @Override
    public TaskResponse update(UUID taskId, UpdateTaskRequest request) {
        validate(taskId, request);

        TaskEntity task = taskRepository.findByTaskIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.canAccess(task.getCreatedByUserId(), task.getAssigneeUserId())) {
            throw new TaskNotFoundException(taskId);
        }
        if (request.isAssigneeUserIdPresent() && !access.canManage(task.getCreatedByUserId())) {
            throw new TaskNotFoundException(taskId);
        }

        applyUpdates(task, request);
        TaskEntity updatedTask = taskRepository.saveAndFlush(task);
        return toResponse(updatedTask);
    }

    private void validate(UUID taskId, UpdateTaskRequest request) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Task update request is required");
        }
        if (!request.hasUpdates()) {
            throw new IllegalArgumentException("At least one task field must be provided");
        }
        if (request.isTitlePresent() && request.getTitle() == null) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (request.isPriorityPresent() && request.getPriority() == null) {
            throw new IllegalArgumentException("Priority must not be null");
        }
    }

    private void applyUpdates(TaskEntity task, UpdateTaskRequest request) {
        if (request.isTitlePresent()) {
            task.setTitle(request.getTitle());
        }
        if (request.isDescriptionPresent()) {
            task.setDescription(request.getDescription());
        }
        if (request.isPriorityPresent()) {
            task.setPriority(request.getPriority());
        }
        if (request.isAssigneeUserIdPresent()) {
            task.setAssigneeUserId(request.getAssigneeUserId());
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

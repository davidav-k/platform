package com.example.task_service.usecase.impl;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskStatusRequest;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.ChangeTaskStatusUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ChangeTaskStatusUseCaseImpl implements ChangeTaskStatusUseCase {

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;

    public ChangeTaskStatusUseCaseImpl(TaskRepository taskRepository,
                                       CurrentUserAccessProvider currentUserAccessProvider) {
        this.taskRepository = taskRepository;
        this.currentUserAccessProvider = currentUserAccessProvider;
    }

    @Override
    public TaskResponse changeStatus(UUID taskId, UpdateTaskStatusRequest request) {
        validate(taskId, request);

        TaskEntity task = taskRepository.findByTaskIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.canAccess(task.getCreatedByUserId(), task.getAssigneeUserId())) {
            throw new TaskNotFoundException(taskId);
        }

        task.setStatus(request.getStatus());
        TaskEntity updatedTask = taskRepository.saveAndFlush(task);
        return toResponse(updatedTask);
    }

    private void validate(UUID taskId, UpdateTaskStatusRequest request) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Task status request is required");
        }
        if (request.getStatus() == null) {
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

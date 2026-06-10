package com.example.task_service.usecase.impl;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.entity.TaskEntity;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.GetTaskUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetTaskUseCaseImpl implements GetTaskUseCase {

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;

    public GetTaskUseCaseImpl(TaskRepository taskRepository, CurrentUserAccessProvider currentUserAccessProvider) {
        this.taskRepository = taskRepository;
        this.currentUserAccessProvider = currentUserAccessProvider;
    }

    @Override
    public TaskResponse getByTaskId(UUID taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }
        TaskEntity task = taskRepository.findByTaskId(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.admin() && !isVisibleTo(task, access.userId())) {
            throw new TaskNotFoundException(taskId);
        }
        return toResponse(task);
    }

    private boolean isVisibleTo(TaskEntity task, UUID userId) {
        return userId.equals(task.getCreatedByUserId()) || userId.equals(task.getAssigneeUserId());
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

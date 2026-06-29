package com.example.task_service.usecase.impl;

import com.example.task_service.entity.TaskEntity;
import com.example.task_service.exception.TaskNotFoundException;
import com.example.task_service.repository.TaskRepository;
import com.example.task_service.security.CurrentUserAccessProvider;
import com.example.task_service.security.CurrentUserAccessProvider.CurrentUserAccess;
import com.example.task_service.usecase.DeleteTaskUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTaskUseCaseImpl implements DeleteTaskUseCase {

    private final TaskRepository taskRepository;
    private final CurrentUserAccessProvider currentUserAccessProvider;

    @Override
    public void delete(UUID taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }

        TaskEntity task = taskRepository.findByTaskIdAndDeletedAtIsNull(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        CurrentUserAccess access = currentUserAccessProvider.currentUserAccess();
        if (!access.canManage(task.getCreatedByUserId())) {
            throw new TaskNotFoundException(taskId);
        }

        task.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        task.setDeletedByUserId(access.userId());
        taskRepository.saveAndFlush(task);
    }
}

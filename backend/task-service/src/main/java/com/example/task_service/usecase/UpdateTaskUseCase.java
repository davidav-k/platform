package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskRequest;

import java.util.UUID;

public interface UpdateTaskUseCase {

    TaskResponse update(UUID taskId, UpdateTaskRequest request);
}

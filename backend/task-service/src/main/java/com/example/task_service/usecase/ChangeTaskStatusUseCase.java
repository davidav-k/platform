package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;
import com.example.task_service.dto.UpdateTaskStatusRequest;

import java.util.UUID;

public interface ChangeTaskStatusUseCase {

    TaskResponse changeStatus(UUID taskId, UpdateTaskStatusRequest request);
}

package com.example.task_service.usecase;

import com.example.task_service.dto.AssignTaskRequest;
import com.example.task_service.dto.TaskResponse;

import java.util.UUID;

public interface AssignTaskUseCase {

    TaskResponse assign(UUID taskId, AssignTaskRequest request);
}

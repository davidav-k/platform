package com.example.task_service.usecase;

import com.example.task_service.dto.TaskResponse;

import java.util.UUID;

public interface GetTaskUseCase {

    TaskResponse getByTaskId(UUID taskId);
}

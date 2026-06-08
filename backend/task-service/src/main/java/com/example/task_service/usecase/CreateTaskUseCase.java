package com.example.task_service.usecase;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;

import java.util.UUID;

public interface CreateTaskUseCase {

    CreateTaskResponse create(CreateTaskRequest request, UUID createdByUserId);
}

package com.example.task_service.usecase;

import java.util.UUID;

public interface DeleteTaskUseCase {

    void delete(UUID taskId);
}

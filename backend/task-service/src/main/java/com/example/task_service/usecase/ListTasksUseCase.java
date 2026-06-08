package com.example.task_service.usecase;

import com.example.task_service.dto.TaskListQuery;
import com.example.task_service.dto.TaskListResponse;

public interface ListTasksUseCase {

    TaskListResponse list(TaskListQuery query);
}

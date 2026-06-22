package com.example.task_service.dto;

import java.util.List;

public record TaskListResponse(
        List<TaskResponse> items,
        PageResponse page
) {
}

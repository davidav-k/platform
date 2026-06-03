package com.example.task_service.dto;

import java.util.List;

public class TaskListResponse {

    private final List<TaskResponse> items;
    private final PageResponse page;

    public TaskListResponse(List<TaskResponse> items, PageResponse page) {
        this.items = items;
        this.page = page;
    }

    public List<TaskResponse> getItems() {
        return items;
    }

    public PageResponse getPage() {
        return page;
    }
}

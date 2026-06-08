package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.enumeration.TaskStatus;

import java.util.UUID;

public class TaskListQuery {

    private final TaskStatus status;
    private final TaskPriority priority;
    private final UUID assigneeUserId;
    private final UUID createdByUserId;
    private final int page;
    private final int size;
    private final String sort;

    public TaskListQuery(TaskStatus status, TaskPriority priority, UUID assigneeUserId,
                         UUID createdByUserId, int page, int size, String sort) {
        this.status = status;
        this.priority = priority;
        this.assigneeUserId = assigneeUserId;
        this.createdByUserId = createdByUserId;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSort() {
        return sort;
    }
}

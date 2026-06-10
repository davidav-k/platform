package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;

    public UpdateTaskStatusRequest() {
    }

    public UpdateTaskStatusRequest(TaskStatus status) {
        this.status = status;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}

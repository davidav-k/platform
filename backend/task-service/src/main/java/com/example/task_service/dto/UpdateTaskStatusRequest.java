package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskStatus;
import jakarta.validation.constraints.NotNull;


public record UpdateTaskStatusRequest(

        @NotNull(message = "Status is required")
        TaskStatus status) {

}

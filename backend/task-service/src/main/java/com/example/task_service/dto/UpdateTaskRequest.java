package com.example.task_service.dto;

import com.example.task_service.enumeration.TaskPriority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateTaskRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "Title must not be blank")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private TaskPriority priority;

    private UUID assigneeUserId;

    private boolean titlePresent;
    private boolean descriptionPresent;
    private boolean priorityPresent;
    private boolean assigneeUserIdPresent;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
        this.assigneeUserIdPresent = true;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public boolean isAssigneeUserIdPresent() {
        return assigneeUserIdPresent;
    }

    public boolean hasUpdates() {
        return titlePresent || descriptionPresent || priorityPresent || assigneeUserIdPresent;
    }
}

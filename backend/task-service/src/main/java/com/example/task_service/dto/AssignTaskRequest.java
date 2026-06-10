package com.example.task_service.dto;

import java.util.UUID;

public class AssignTaskRequest {

    private UUID assigneeUserId;
    private boolean assigneeUserIdPresent;

    public AssignTaskRequest() {
    }

    public AssignTaskRequest(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
        this.assigneeUserIdPresent = true;
    }

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(UUID assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
        this.assigneeUserIdPresent = true;
    }

    public boolean isAssigneeUserIdPresent() {
        return assigneeUserIdPresent;
    }
}

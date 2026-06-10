package com.example.task_service.notification.dto;

import java.util.UUID;

public class CreateNotificationRequest {

    private final UUID recipientUserId;
    private final String type;
    private final String title;
    private final String message;
    private final String sourceService;
    private final String sourceEntityType;
    private final UUID sourceEntityId;

    public CreateNotificationRequest(UUID recipientUserId, String type, String title,
                                     String message, String sourceService,
                                     String sourceEntityType, UUID sourceEntityId) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.sourceService = sourceService;
        this.sourceEntityType = sourceEntityType;
        this.sourceEntityId = sourceEntityId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public UUID getSourceEntityId() {
        return sourceEntityId;
    }
}

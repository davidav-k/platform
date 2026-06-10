package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateSystemNotificationRequest {

    @NotNull(message = "Recipient user ID is required")
    private UUID recipientUserId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Message must not be blank")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    private String message;

    @NotBlank(message = "Source service must not be blank")
    @Size(max = 100, message = "Source service must not exceed 100 characters")
    private String sourceService;

    @NotBlank(message = "Source entity type must not be blank")
    @Size(max = 100, message = "Source entity type must not exceed 100 characters")
    private String sourceEntityType;

    @NotNull(message = "Source entity ID is required")
    private UUID sourceEntityId;

    public CreateSystemNotificationRequest() {
    }

    public CreateSystemNotificationRequest(UUID recipientUserId, NotificationType type,
                                           String title, String message, String sourceService,
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

    public void setRecipientUserId(UUID recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public void setSourceEntityType(String sourceEntityType) {
        this.sourceEntityType = sourceEntityType;
    }

    public UUID getSourceEntityId() {
        return sourceEntityId;
    }

    public void setSourceEntityId(UUID sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }
}

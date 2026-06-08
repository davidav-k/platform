package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateNotificationRequest {

    @NotNull(message = "Recipient user ID is required")
    private UUID recipientUserId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Body must not be blank")
    @Size(max = 5000, message = "Body must not exceed 5000 characters")
    private String body;

    public CreateNotificationRequest() {
    }

    public CreateNotificationRequest(UUID recipientUserId, NotificationType type,
                                     NotificationChannel channel, String subject, String body) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
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

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

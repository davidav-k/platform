package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationResponse {

    private final UUID notificationId;
    private final UUID recipientUserId;
    private final NotificationType type;
    private final NotificationChannel channel;
    private final String subject;
    private final String body;
    private final NotificationStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime sentAt;
    private final String failureReason;

    public NotificationResponse(UUID notificationId, UUID recipientUserId, NotificationType type,
                                NotificationChannel channel, String subject, String body,
                                NotificationStatus status, OffsetDateTime createdAt,
                                OffsetDateTime updatedAt, OffsetDateTime sentAt,
                                String failureReason) {
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sentAt = sentAt;
        this.failureReason = failureReason;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

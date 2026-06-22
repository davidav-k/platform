package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(

        UUID notificationId,
        UUID recipientUserId,
        NotificationType type,
        NotificationChannel channel,
        String subject,
        String body,
        NotificationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime sentAt,
        String failureReason) {
}

package com.example.notification_service.dto;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;

import java.util.UUID;

public record NotificationListQuery(
        UUID recipientUserId,
        NotificationStatus status,
        NotificationChannel channel,
        NotificationType type,
        int page,
        int size,
        String sort
) {}

package com.example.notification_service.usecase;

import com.example.notification_service.dto.NotificationResponse;

import java.util.UUID;

public interface GetNotificationUseCase {

    NotificationResponse getByNotificationId(UUID notificationId);
}

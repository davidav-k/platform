package com.example.notification_service.usecase;

import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;

public interface CreateSystemNotificationUseCase {

    NotificationResponse create(CreateSystemNotificationRequest request);
}

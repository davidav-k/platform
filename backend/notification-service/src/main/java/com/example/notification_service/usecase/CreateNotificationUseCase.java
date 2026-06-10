package com.example.notification_service.usecase;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;

public interface CreateNotificationUseCase {

    NotificationResponse create(CreateNotificationRequest request);
}

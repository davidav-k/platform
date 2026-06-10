package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;

public interface NotificationClient {

    void createNotification(CreateNotificationRequest request);
}

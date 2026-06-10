package com.example.notification_service.usecase;

import com.example.notification_service.dto.NotificationListQuery;
import com.example.notification_service.dto.NotificationListResponse;

public interface ListNotificationsUseCase {

    NotificationListResponse list(NotificationListQuery query);
}

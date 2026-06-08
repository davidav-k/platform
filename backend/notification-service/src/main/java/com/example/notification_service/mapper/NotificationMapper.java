package com.example.notification_service.mapper;

import com.example.notification_service.dto.NotificationPreferenceResponse;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.entity.NotificationPreferenceEntity;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getNotificationId(),
                entity.getRecipientUserId(),
                entity.getType(),
                entity.getChannel(),
                entity.getSubject(),
                entity.getBody(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSentAt(),
                entity.getFailureReason()
        );
    }

    public static NotificationPreferenceResponse toResponse(NotificationPreferenceEntity entity) {
        return new NotificationPreferenceResponse(
                entity.getUserId(),
                entity.isEmailEnabled(),
                entity.isInAppEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

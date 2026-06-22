package com.example.task_service.notification.dto;

import java.util.UUID;

public record CreateNotificationRequest(UUID recipientUserId, String type, String title, String message,
                                        String sourceService, String sourceEntityType, UUID sourceEntityId) {

}

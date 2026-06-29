package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSystemNotificationRequest(
        @NotNull(message = "Recipient user ID is required")
        UUID recipientUserId,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Message must not be blank")
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        String message,

        @NotBlank(message = "Source service must not be blank")
        @Size(max = 100, message = "Source service must not exceed 100 characters")
        String sourceService,

        @NotBlank(message = "Source entity type must not be blank")
        @Size(max = 100, message = "Source entity type must not exceed 100 characters")
        String sourceEntityType,

        @NotNull(message = "Source entity ID is required")
        UUID sourceEntityId
) {}

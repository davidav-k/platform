package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.With;


import java.util.UUID;

@With
public record CreateNotificationRequest (

    @NotNull(message = "Recipient user ID is required")
     UUID recipientUserId,

    @NotNull(message = "Notification type is required")
     NotificationType type,

    @NotNull(message = "Notification channel is required")
     NotificationChannel channel,

    @Size(max = 255, message = "Subject must not exceed 255 characters")
     String subject,

    @NotBlank(message = "Body must not be blank")
    @Size(max = 5000, message = "Body must not exceed 5000 characters")
     String body){

}

package com.example.notification_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationPreferenceResponse(UUID userId, boolean emailEnabled, boolean inAppEnabled,
                                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}

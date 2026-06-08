package com.example.notification_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationPreferenceResponse {

    private final UUID userId;
    private final boolean emailEnabled;
    private final boolean inAppEnabled;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public NotificationPreferenceResponse(UUID userId, boolean emailEnabled, boolean inAppEnabled,
                                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.inAppEnabled = inAppEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

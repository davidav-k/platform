package com.example.notification_service.dto;

import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;

import java.util.UUID;

public class NotificationListQuery {

    private final UUID recipientUserId;
    private final NotificationStatus status;
    private final NotificationChannel channel;
    private final NotificationType type;
    private final int page;
    private final int size;
    private final String sort;

    public NotificationListQuery(UUID recipientUserId, NotificationStatus status,
                                 NotificationChannel channel, NotificationType type,
                                 int page, int size, String sort) {
        this.recipientUserId = recipientUserId;
        this.status = status;
        this.channel = channel;
        this.type = type;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSort() {
        return sort;
    }
}

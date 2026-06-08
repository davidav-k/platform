package com.example.task_service.notification.dto;

import java.util.UUID;

public class CreateNotificationRequest {

    private final UUID recipientUserId;
    private final String type;
    private final String channel;
    private final String subject;
    private final String body;

    public CreateNotificationRequest(UUID recipientUserId, String type, String channel,
                                     String subject, String body) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}

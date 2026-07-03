package com.example.notification_service.outbox;

import com.example.notification_service.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}

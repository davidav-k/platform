package com.example.user_service.outbox;

import com.example.user_service.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}

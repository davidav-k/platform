package com.example.ai_service.outbox;

import com.example.ai_service.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}

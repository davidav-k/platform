package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;

public interface OutboxEventPublisher {

    void publish(OutboxEventEntity event);
}

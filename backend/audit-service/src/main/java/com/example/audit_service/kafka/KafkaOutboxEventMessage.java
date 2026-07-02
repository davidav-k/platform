package com.example.audit_service.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Task-service outbox event envelope published to the shared task-events topic.
 */
public record KafkaOutboxEventMessage(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        OffsetDateTime occurredAt,
        int eventVersion,
        String payload
) {
}

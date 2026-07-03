package com.example.audit_service.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbox event envelope shared by platform services that publish audit events.
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

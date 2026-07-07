package com.example.ai_service.outbox.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

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

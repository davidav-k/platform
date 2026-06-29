package com.example.notification_service.kafka;

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

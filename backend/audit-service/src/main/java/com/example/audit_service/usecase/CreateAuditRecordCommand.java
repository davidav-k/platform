package com.example.audit_service.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Internal input model for persisting a normalized audit record.
 */
public record CreateAuditRecordCommand(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String sourceService,
        UUID actorUserId,
        String actorEmail,
        String action,
        String payload,
        OffsetDateTime occurredAt
) {
}

package com.example.audit_service.normalization;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Internal representation shared by event-source normalizers and audit
 * persistence. It is not an API contract.
 */
public record NormalizedAuditEvent(
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

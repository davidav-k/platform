package com.example.audit_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditResponseDto(
        UUID auditId,
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String sourceService,
        UUID actorUserId,
        String actorEmail,
        String action,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {
}

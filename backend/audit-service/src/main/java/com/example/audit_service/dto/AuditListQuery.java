package com.example.audit_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditListQuery(
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String sourceService,
        UUID actorUserId,
        String action,
        OffsetDateTime from,
        OffsetDateTime to,
        int page,
        int size,
        String sort
) {
}

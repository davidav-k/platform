package com.example.audit_service.mapper;

import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.entity.AuditRecordEntity;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditResponseDto toResponse(AuditRecordEntity entity) {
        return new AuditResponseDto(
                entity.getAuditId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getSourceService(),
                entity.getActorUserId(),
                entity.getActorEmail(),
                entity.getAction(),
                entity.getOccurredAt(),
                entity.getCreatedAt()
        );
    }
}

package com.example.audit_service.usecase.impl;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.repository.AuditRecordRepository;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAuditRecordUseCaseImpl implements CreateAuditRecordUseCase {

    private final AuditRecordRepository auditRecordRepository;

    public CreateAuditRecordUseCaseImpl(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @Override
    @Transactional
    public boolean create(NormalizedAuditEvent event) {
        validate(event);

        if (auditRecordRepository.existsByEventId(event.eventId())) {
            return false;
        }

        AuditRecordEntity auditRecord = new AuditRecordEntity(
                null,
                event.eventId(),
                event.eventType().strip(),
                event.aggregateType().strip(),
                event.aggregateId(),
                event.sourceService().strip(),
                event.actorUserId(),
                trimToNull(event.actorEmail()),
                event.action().strip(),
                event.payload(),
                event.occurredAt()
        );

        auditRecordRepository.save(auditRecord);
        return true;
    }

    private void validate(NormalizedAuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Normalized audit event is required");
        }
        if (event.eventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (isBlank(event.eventType())) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (isBlank(event.aggregateType())) {
            throw new IllegalArgumentException("Aggregate type is required");
        }
        if (event.aggregateId() == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        if (isBlank(event.sourceService())) {
            throw new IllegalArgumentException("Source service is required");
        }
        if (isBlank(event.action())) {
            throw new IllegalArgumentException("Action is required");
        }
        if (isBlank(event.payload())) {
            throw new IllegalArgumentException("Payload is required");
        }
        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("Occurred at is required");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.example.audit_service.usecase.impl;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.repository.AuditRecordRepository;
import com.example.audit_service.usecase.CreateAuditRecordCommand;
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
    public boolean create(CreateAuditRecordCommand command) {
        validate(command);

        if (auditRecordRepository.existsByEventId(command.eventId())) {
            return false;
        }

        AuditRecordEntity auditRecord = new AuditRecordEntity(
                null,
                command.eventId(),
                command.eventType().strip(),
                command.aggregateType().strip(),
                command.aggregateId(),
                command.sourceService().strip(),
                command.actorUserId(),
                trimToNull(command.actorEmail()),
                command.action().strip(),
                command.payload(),
                command.occurredAt()
        );

        auditRecordRepository.save(auditRecord);
        return true;
    }

    private void validate(CreateAuditRecordCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Create audit record command is required");
        }
        if (command.eventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (isBlank(command.eventType())) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (isBlank(command.aggregateType())) {
            throw new IllegalArgumentException("Aggregate type is required");
        }
        if (command.aggregateId() == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        if (isBlank(command.sourceService())) {
            throw new IllegalArgumentException("Source service is required");
        }
        if (isBlank(command.action())) {
            throw new IllegalArgumentException("Action is required");
        }
        if (isBlank(command.payload())) {
            throw new IllegalArgumentException("Payload is required");
        }
        if (command.occurredAt() == null) {
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

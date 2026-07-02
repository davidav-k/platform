package com.example.audit_service.usecase;

public interface CreateAuditRecordUseCase {

    /**
     * Persists a new audit record.
     *
     * @return {@code true} when a record was created, or {@code false} when
     * the source event was already stored
     */
    boolean create(CreateAuditRecordCommand command);
}

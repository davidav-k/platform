package com.example.audit_service.usecase;

import com.example.audit_service.normalization.NormalizedAuditEvent;

public interface CreateAuditRecordUseCase {

    /**
     * Persists a new audit record.
     *
     * @return {@code true} when a record was created, or {@code false} when
     * the source event was already stored
     */
    boolean create(NormalizedAuditEvent event);
}

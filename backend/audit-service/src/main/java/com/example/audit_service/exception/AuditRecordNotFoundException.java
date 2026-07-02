package com.example.audit_service.exception;

import java.util.UUID;

public class AuditRecordNotFoundException extends RuntimeException {

    public AuditRecordNotFoundException(UUID auditId) {
        super("Audit record not found: " + auditId);
    }
}

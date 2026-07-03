package com.example.audit_service.service;

import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;

import java.util.UUID;

public interface AuditQueryService {

    AuditListResponse findAll(AuditListQuery query);

    AuditResponseDto getByAuditId(UUID auditId);
}

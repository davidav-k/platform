package com.example.audit_service.dto;

import java.util.List;

public record AuditListResponse(
        List<AuditResponseDto> items,
        PageResponse page
) {
}

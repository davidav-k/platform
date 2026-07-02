package com.example.audit_service.controller;

import com.example.audit_service.domain.Response;
import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.service.AuditQueryService;
import com.example.audit_service.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping
    public ResponseEntity<Response> listAuditRecords(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            HttpServletRequest request
    ) {
        AuditListResponse records = auditQueryService.findAll(new AuditListQuery(
                eventType,
                aggregateType,
                aggregateId,
                sourceService,
                actorUserId,
                action,
                from,
                to,
                page,
                size,
                sort
        ));
        return ResponseEntity.ok(RequestUtils.getResponse(
                request,
                Map.of("items", records.items(), "page", records.page()),
                "Audit records retrieved successfully.",
                HttpStatus.OK
        ));
    }

    @GetMapping("/{auditId}")
    public ResponseEntity<Response> getAuditRecord(@PathVariable UUID auditId,
                                                   HttpServletRequest request) {
        AuditResponseDto auditRecord = auditQueryService.getByAuditId(auditId);
        return ResponseEntity.ok(RequestUtils.getResponse(
                request,
                Map.of("audit", auditRecord),
                "Audit record retrieved successfully.",
                HttpStatus.OK
        ));
    }
}

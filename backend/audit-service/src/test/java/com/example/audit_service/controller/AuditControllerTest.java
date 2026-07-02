package com.example.audit_service.controller;

import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.dto.PageResponse;
import com.example.audit_service.exception.AuditRecordNotFoundException;
import com.example.audit_service.service.AuditQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    private static final UUID AUDIT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID AGGREGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID ACTOR_USER_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-07-02T08:30:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-02T08:31:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @Test
    void listsAuditRecordsWithPaginationAndFilters() throws Exception {
        AuditResponseDto audit = auditResponse();
        when(auditQueryService.findAll(any(AuditListQuery.class)))
                .thenReturn(new AuditListResponse(List.of(audit), new PageResponse(1, 10, 21, 3)));

        mockMvc.perform(get("/api/v1/audit")
                        .param("eventType", "TASK_UPDATED")
                        .param("aggregateType", "TASK")
                        .param("aggregateId", AGGREGATE_ID.toString())
                        .param("sourceService", "task-service")
                        .param("actorUserId", ACTOR_USER_ID.toString())
                        .param("action", "UPDATE_TASK")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-07-03T00:00:00Z")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "occurredAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Audit records retrieved successfully."))
                .andExpect(jsonPath("$.data.items[0].auditId").value(AUDIT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].eventType").value("TASK_UPDATED"))
                .andExpect(jsonPath("$.data.items[0].aggregateType").value("TASK"))
                .andExpect(jsonPath("$.data.items[0].aggregateId").value(AGGREGATE_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].sourceService").value("task-service"))
                .andExpect(jsonPath("$.data.items[0].actorUserId").value(ACTOR_USER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].actorEmail").value("actor@example.com"))
                .andExpect(jsonPath("$.data.items[0].action").value("UPDATE_TASK"))
                .andExpect(jsonPath("$.data.items[0].occurredAt").value("2026-07-02T08:30:00Z"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-07-02T08:31:00Z"))
                .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].payload").doesNotExist())
                .andExpect(jsonPath("$.data.page.number").value(1))
                .andExpect(jsonPath("$.data.page.size").value(10))
                .andExpect(jsonPath("$.data.page.totalElements").value(21))
                .andExpect(jsonPath("$.data.page.totalPages").value(3));

        verify(auditQueryService).findAll(argThat(query ->
                "TASK_UPDATED".equals(query.eventType())
                        && "TASK".equals(query.aggregateType())
                        && AGGREGATE_ID.equals(query.aggregateId())
                        && "task-service".equals(query.sourceService())
                        && ACTOR_USER_ID.equals(query.actorUserId())
                        && "UPDATE_TASK".equals(query.action())
                        && OffsetDateTime.parse("2026-07-01T00:00:00Z").equals(query.from())
                        && OffsetDateTime.parse("2026-07-03T00:00:00Z").equals(query.to())
                        && query.page() == 1
                        && query.size() == 10
                        && "occurredAt,asc".equals(query.sort())
        ));
    }

    @Test
    void getsAuditRecordByAuditId() throws Exception {
        when(auditQueryService.getByAuditId(AUDIT_ID)).thenReturn(auditResponse());

        mockMvc.perform(get("/api/v1/audit/{auditId}", AUDIT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audit.auditId").value(AUDIT_ID.toString()))
                .andExpect(jsonPath("$.message").value("Audit record retrieved successfully."));
    }

    @Test
    void returnsNotFoundWhenAuditRecordDoesNotExist() throws Exception {
        when(auditQueryService.getByAuditId(AUDIT_ID))
                .thenThrow(new AuditRecordNotFoundException(AUDIT_ID));

        mockMvc.perform(get("/api/v1/audit/{auditId}", AUDIT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Audit record not found."));
    }

    @Test
    void returnsBadRequestForInvalidUuidFilter() throws Exception {
        mockMvc.perform(get("/api/v1/audit").param("aggregateId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Provided arguments are not valid"))
                .andExpect(jsonPath("$.data.aggregateId").value("Value has an invalid format"));
    }

    @Test
    void returnsBadRequestWhenQueryValidationFails() throws Exception {
        when(auditQueryService.findAll(argThat(query -> query.page() < 0)))
                .thenThrow(new IllegalArgumentException("Page must be greater than or equal to 0"));

        mockMvc.perform(get("/api/v1/audit").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page must be greater than or equal to 0"));
    }

    private AuditResponseDto auditResponse() {
        return new AuditResponseDto(
                AUDIT_ID,
                EVENT_ID,
                "TASK_UPDATED",
                "TASK",
                AGGREGATE_ID,
                "task-service",
                ACTOR_USER_ID,
                "actor@example.com",
                "UPDATE_TASK",
                OCCURRED_AT,
                CREATED_AT
        );
    }
}

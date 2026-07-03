package com.example.audit_service.security;

import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.dto.PageResponse;
import com.example.audit_service.repository.AuditRecordRepository;
import com.example.audit_service.service.AuditQueryService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "audit.kafka.enabled=false",
        "server.port=0",
        "management.endpoints.web.exposure.include=health,info",
        "jwt.secret=" + AuditSecurityIntegrationTest.TEST_SECRET,
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@AutoConfigureMockMvc
class AuditSecurityIntegrationTest {

    static final String TEST_SECRET =
            "QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ==";

    private static final UUID AUDIT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID AGGREGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @MockitoBean
    private AuditRecordRepository auditRecordRepository;

    @BeforeEach
    void setUp() {
        AuditResponseDto response = auditResponse();
        when(auditQueryService.findAll(any(AuditListQuery.class)))
                .thenReturn(new AuditListResponse(List.of(response), new PageResponse(0, 20, 1, 1)));
        when(auditQueryService.getByAuditId(AUDIT_ID)).thenReturn(response);
    }

    @Test
    void unauthenticatedAuditRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void regularUserAuditRequestReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void adminCanAccessAuditList() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].auditId").value(AUDIT_ID.toString()));
    }

    @Test
    void superAdminCanAccessAuditList() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].auditId").value(AUDIT_ID.toString()));
    }

    @Test
    void adminCanAccessAuditRecordByAuditId() throws Exception {
        mockMvc.perform(get("/api/v1/audit/{auditId}", AUDIT_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audit.auditId").value(AUDIT_ID.toString()));
    }

    @Test
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void actuatorHealthAndInfoRemainPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    private String accessToken(String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("authorities", "audit:read")
                .claim("role", role)
                .issuedAt(Date.from(now.minusSeconds(1)))
                .notBefore(Date.from(now.minusSeconds(1)))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS512)
                .compact();
    }

    private AuditResponseDto auditResponse() {
        return new AuditResponseDto(
                AUDIT_ID,
                EVENT_ID,
                "TASK_UPDATED",
                "TASK",
                AGGREGATE_ID,
                "task-service",
                UUID.randomUUID(),
                "actor@example.com",
                "UPDATE_TASK",
                OffsetDateTime.parse("2026-07-02T08:30:00Z"),
                OffsetDateTime.parse("2026-07-02T08:31:00Z")
        );
    }
}

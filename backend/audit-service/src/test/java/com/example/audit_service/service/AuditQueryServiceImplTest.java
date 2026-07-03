package com.example.audit_service.service;

import com.example.audit_service.dto.AuditListQuery;
import com.example.audit_service.dto.AuditListResponse;
import com.example.audit_service.dto.AuditResponseDto;
import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.exception.AuditRecordNotFoundException;
import com.example.audit_service.repository.AuditRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "audit.kafka.enabled=false",
        "jwt.secret=QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ==",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@ActiveProfiles("test")
@Testcontainers
class AuditQueryServiceImplTest {

    private static final OffsetDateTime FIRST_OCCURRED_AT =
            OffsetDateTime.parse("2026-07-01T08:00:00Z");
    private static final OffsetDateTime SECOND_OCCURRED_AT =
            OffsetDateTime.parse("2026-07-02T08:00:00Z");
    private static final OffsetDateTime THIRD_OCCURRED_AT =
            OffsetDateTime.parse("2026-07-03T08:00:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("audit_query_service_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditQueryService auditQueryService;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID firstAggregateId;
    private UUID secondAggregateId;
    private UUID firstActorUserId;
    private UUID secondActorUserId;
    private UUID createdAuditId;

    @BeforeEach
    void setUp() {
        auditRecordRepository.deleteAll();
        firstAggregateId = UUID.randomUUID();
        secondAggregateId = UUID.randomUUID();
        firstActorUserId = UUID.randomUUID();
        secondActorUserId = UUID.randomUUID();

        List<AuditRecordEntity> saved = auditRecordRepository.saveAllAndFlush(List.of(
                auditRecord("TASK_CREATED", "TASK", firstAggregateId, "task-service",
                        firstActorUserId, "CREATE_TASK", FIRST_OCCURRED_AT),
                auditRecord("TASK_UPDATED", "TASK", firstAggregateId, "task-service",
                        firstActorUserId, "UPDATE_TASK", SECOND_OCCURRED_AT),
                auditRecord("TASK_DELETED", "TASK", secondAggregateId, "task-service",
                        secondActorUserId, "DELETE_TASK", THIRD_OCCURRED_AT)
        ));
        createdAuditId = saved.get(0).getAuditId();
    }

    @Test
    void listsRecordsWithPaginationAndDefaultSort() {
        AuditListResponse response = auditQueryService.findAll(
                query(null, null, null, null, null, null, null, null, 0, 2, null)
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).extracting(AuditResponseDto::eventType)
                .containsExactly("TASK_DELETED", "TASK_UPDATED");
        assertThat(response.page().number()).isZero();
        assertThat(response.page().size()).isEqualTo(2);
        assertThat(response.page().totalElements()).isEqualTo(3);
        assertThat(response.page().totalPages()).isEqualTo(2);
    }

    @Test
    void filtersBySupportedFields() {
        assertThat(find(query("TASK_CREATED", null, null, null, null, null,
                null, null, 0, 20, null))).singleElement()
                .extracting(AuditResponseDto::eventType).isEqualTo("TASK_CREATED");

        assertThat(find(query(null, "TASK", null, null, null, null,
                null, null, 0, 20, null))).hasSize(3);

        assertThat(find(query(null, null, firstAggregateId, null, null, null,
                null, null, 0, 20, null))).hasSize(2)
                .allMatch(item -> firstAggregateId.equals(item.aggregateId()));

        assertThat(find(query(null, null, null, "task-service", null, null,
                null, null, 0, 20, null))).hasSize(3)
                .allMatch(item -> "task-service".equals(item.sourceService()));

        assertThat(find(query(null, null, null, null, firstActorUserId, null,
                null, null, 0, 20, null))).hasSize(2)
                .allMatch(item -> firstActorUserId.equals(item.actorUserId()));

        assertThat(find(query(null, null, null, null, null, "DELETE_TASK",
                null, null, 0, 20, null))).singleElement()
                .extracting(AuditResponseDto::action).isEqualTo("DELETE_TASK");
    }

    @Test
    void filtersByInclusiveOccurredAtRange() {
        List<AuditResponseDto> items = find(query(
                null, null, null, null, null, null,
                SECOND_OCCURRED_AT, SECOND_OCCURRED_AT, 0, 20, null
        ));

        assertThat(items).singleElement()
                .extracting(AuditResponseDto::eventType).isEqualTo("TASK_UPDATED");
    }

    @Test
    void sortsByAllowedFieldAndDirection() {
        List<AuditResponseDto> items = find(query(
                null, null, null, null, null, null,
                null, null, 0, 20, "eventType,asc"
        ));

        assertThat(items).extracting(AuditResponseDto::eventType)
                .containsExactly("TASK_CREATED", "TASK_DELETED", "TASK_UPDATED");
    }

    @Test
    void getsByAuditIdWithoutExposingEntityInternals() throws Exception {
        AuditResponseDto response = auditQueryService.getByAuditId(createdAuditId);

        assertThat(response.auditId()).isEqualTo(createdAuditId);
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).doesNotContain("\"id\"").doesNotContain("\"payload\"");
    }

    @Test
    void throwsNotFoundForUnknownAuditId() {
        UUID unknownAuditId = UUID.randomUUID();

        assertThatThrownBy(() -> auditQueryService.getByAuditId(unknownAuditId))
                .isInstanceOf(AuditRecordNotFoundException.class)
                .hasMessage("Audit record not found: " + unknownAuditId);
    }

    @Test
    void rejectsInvalidPaginationRangeAndSort() {
        assertThatThrownBy(() -> auditQueryService.findAll(query(
                null, null, null, null, null, null,
                null, null, -1, 20, null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be greater than or equal to 0");

        assertThatThrownBy(() -> auditQueryService.findAll(query(
                null, null, null, null, null, null,
                null, null, 0, 101, null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be between 1 and 100");

        assertThatThrownBy(() -> auditQueryService.findAll(query(
                null, null, null, null, null, null,
                THIRD_OCCURRED_AT, FIRST_OCCURRED_AT, 0, 20, null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("From must be before or equal to to");

        assertThatThrownBy(() -> auditQueryService.findAll(query(
                null, null, null, null, null, null,
                null, null, 0, 20, "id,asc"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported sort field: id");
    }

    private List<AuditResponseDto> find(AuditListQuery query) {
        return auditQueryService.findAll(query).items();
    }

    private AuditListQuery query(String eventType, String aggregateType, UUID aggregateId,
                                 String sourceService, UUID actorUserId, String action,
                                 OffsetDateTime from, OffsetDateTime to,
                                 int page, int size, String sort) {
        return new AuditListQuery(
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
        );
    }

    private AuditRecordEntity auditRecord(String eventType, String aggregateType,
                                          UUID aggregateId, String sourceService,
                                          UUID actorUserId, String action,
                                          OffsetDateTime occurredAt) {
        return new AuditRecordEntity(
                null,
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                sourceService,
                actorUserId,
                "actor@example.com",
                action,
                "{}",
                occurredAt
        );
    }
}

package com.example.audit_service.kafka;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.normalization.TaskAuditEventNormalizer;
import com.example.audit_service.repository.AuditRecordRepository;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
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

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "audit.kafka.enabled=false",
        "jwt.secret=QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ=="
})
@ActiveProfiles("test")
@Testcontainers
class TaskEventConsumerPersistenceIntegrationTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CREATED_BY_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("audit_task_event_consumer_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskAuditEventNormalizer taskAuditEventNormalizer;

    @Autowired
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    private TaskEventConsumer consumer;

    @BeforeEach
    void setUp() {
        auditRecordRepository.deleteAll();
        consumer = new TaskEventConsumer(objectMapper, taskAuditEventNormalizer, createAuditRecordUseCase);
    }

    @Test
    void duplicateTaskCreatedDeliveryPersistsExactlyOneAuditRecord() throws Exception {
        String message = objectMapper.writeValueAsString(event());

        consumer.consume(message);
        consumer.consume(message);

        List<AuditRecordEntity> auditRecords = auditRecordRepository.findAll();
        assertThat(auditRecords).hasSize(1);
        AuditRecordEntity auditRecord = auditRecords.get(0);
        assertThat(auditRecord.getEventId()).isEqualTo(EVENT_ID);
        assertThat(auditRecord.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(auditRecord.getAggregateType()).isEqualTo("TASK");
        assertThat(auditRecord.getAggregateId()).isEqualTo(TASK_ID);
        assertThat(auditRecord.getSourceService()).isEqualTo("task-service");
        assertThat(auditRecord.getActorUserId()).isEqualTo(CREATED_BY_ID);
        assertThat(auditRecord.getActorEmail()).isNull();
        assertThat(auditRecord.getAction()).isEqualTo("CREATE_TASK");
        assertThat(auditRecord.getPayload()).contains(TASK_ID.toString());
    }

    private KafkaOutboxEventMessage event() {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                "TASK_CREATED",
                "TASK",
                TASK_ID,
                OffsetDateTime.parse("2026-07-02T08:30:00Z"),
                1,
                "{\"taskId\":\"%s\",\"createdByUserId\":\"%s\"}"
                        .formatted(TASK_ID, CREATED_BY_ID)
        );
    }
}

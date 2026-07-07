package com.example.audit_service.kafka;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.normalization.AiAuditEventNormalizer;
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
class AiAuditEventConsumerPersistenceIntegrationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OPERATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ACTOR_USER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
                    .withDatabaseName("audit_ai_event_consumer_test")
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
    private AiAuditEventNormalizer aiAuditEventNormalizer;

    @Autowired
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    private AiAuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        auditRecordRepository.deleteAll();
        consumer = new AiAuditEventConsumer(
                objectMapper,
                aiAuditEventNormalizer,
                createAuditRecordUseCase
        );
    }

    @Test
    void duplicateDeliveryPersistsOneAiAuditRecordWithoutGeneratedContent() throws Exception {
        String message = objectMapper.writeValueAsString(event());

        consumer.consume(message);
        consumer.consume(message);

        List<AuditRecordEntity> records = auditRecordRepository.findAll();
        assertThat(records).hasSize(1);
        AuditRecordEntity record = records.get(0);
        assertThat(record.getEventId()).isEqualTo(EVENT_ID);
        assertThat(record.getEventType()).isEqualTo("AI_TASK_SUMMARIZED");
        assertThat(record.getAggregateType()).isEqualTo("AI_OPERATION");
        assertThat(record.getAggregateId()).isEqualTo(OPERATION_ID);
        assertThat(record.getSourceService()).isEqualTo("ai-service");
        assertThat(record.getActorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(record.getActorEmail()).isNull();
        assertThat(record.getAction()).isEqualTo("SUMMARIZE_TASK");
        assertThat(record.getPayload())
                .contains("temporary-noop")
                .contains("not-configured")
                .contains("actorUserId")
                .doesNotContain("actorEmail")
                .doesNotContain("title")
                .doesNotContain("description")
                .doesNotContain("prompt")
                .doesNotContain("generated")
                .doesNotContain("jwt")
                .doesNotContain("token");
    }

    private KafkaOutboxEventMessage event() {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                "AI_TASK_SUMMARIZED",
                "AI_OPERATION",
                OPERATION_ID,
                OffsetDateTime.parse("2026-07-06T10:15:30Z"),
                1,
                """
                        {
                          "operationId":"%s",
                          "operationType":"AI_TASK_SUMMARIZED",
                          "actorUserId":"%s",
                          "occurredAt":"2026-07-06T10:15:30Z",
                          "providerName":"temporary-noop",
                          "modelName":"not-configured"
                        }
                        """.formatted(OPERATION_ID, ACTOR_USER_ID)
        );
    }
}

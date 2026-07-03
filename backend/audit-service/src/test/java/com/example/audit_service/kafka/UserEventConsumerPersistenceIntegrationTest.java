package com.example.audit_service.kafka;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.normalization.UserAuditEventNormalizer;
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
class UserEventConsumerPersistenceIntegrationTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("audit_user_event_consumer_test")
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
    private UserAuditEventNormalizer userAuditEventNormalizer;

    @Autowired
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    private UserEventConsumer consumer;

    @BeforeEach
    void setUp() {
        auditRecordRepository.deleteAll();
        consumer = new UserEventConsumer(objectMapper, userAuditEventNormalizer, createAuditRecordUseCase);
    }

    @Test
    void duplicateDeliveryPersistsOneSanitizedUserAuditRecord() throws Exception {
        String message = objectMapper.writeValueAsString(event());

        consumer.consume(message);
        consumer.consume(message);

        List<AuditRecordEntity> auditRecords = auditRecordRepository.findAll();
        assertThat(auditRecords).hasSize(1);
        AuditRecordEntity auditRecord = auditRecords.get(0);
        assertThat(auditRecord.getEventId()).isEqualTo(EVENT_ID);
        assertThat(auditRecord.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(auditRecord.getAggregateType()).isEqualTo("USER");
        assertThat(auditRecord.getAggregateId()).isEqualTo(USER_ID);
        assertThat(auditRecord.getSourceService()).isEqualTo("user-service");
        assertThat(auditRecord.getActorUserId()).isEqualTo(USER_ID);
        assertThat(auditRecord.getActorEmail()).isEqualTo("user@example.com");
        assertThat(auditRecord.getAction()).isEqualTo("REGISTER_USER");
        assertThat(auditRecord.getPayload())
                .contains("user@example.com")
                .doesNotContain("do-not-store")
                .doesNotContain("reset-token");
    }

    private KafkaOutboxEventMessage event() {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                "USER_REGISTERED",
                "USER",
                USER_ID,
                OffsetDateTime.parse("2026-07-03T08:30:00Z"),
                1,
                """
                    {
                      "userId":"%s",
                      "email":"user@example.com",
                      "password":"placeholder_password",
                      "nested":{"resetToken":"reset-token"}
                    }
                    """.formatted(USER_ID)
        );
    }
}

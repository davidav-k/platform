package com.example.audit_service.repository;

import com.example.audit_service.entity.AuditRecordEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditRecordRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("audit_record_repository_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsAuditRecordWithJsonbTimestampsAndVersion() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-07-02T08:30:00Z");

        AuditRecordEntity saved = auditRecordRepository.saveAndFlush(new AuditRecordEntity(
                null,
                eventId,
                "TASK_CREATED",
                "TASK",
                aggregateId,
                "task-service",
                actorUserId,
                "actor@example.com",
                "CREATE",
                "{\"taskId\":\"%s\"}".formatted(aggregateId),
                occurredAt
        ));

        UUID auditId = saved.getAuditId();
        entityManager.clear();

        AuditRecordEntity found = auditRecordRepository.findByAuditId(auditId).orElseThrow();

        assertThat(found.getEventId()).isEqualTo(eventId);
        assertThat(found.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(found.getAggregateType()).isEqualTo("TASK");
        assertThat(found.getAggregateId()).isEqualTo(aggregateId);
        assertThat(found.getSourceService()).isEqualTo("task-service");
        assertThat(found.getActorUserId()).isEqualTo(actorUserId);
        assertThat(found.getActorEmail()).isEqualTo("actor@example.com");
        assertThat(found.getAction()).isEqualTo("CREATE");
        assertThat(found.getPayload()).contains(aggregateId.toString());
        assertThat(found.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getVersion()).isZero();
        assertThat(auditRecordRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    void rejectsDuplicateEventId() {
        UUID eventId = UUID.randomUUID();

        auditRecordRepository.saveAndFlush(auditRecord(null, eventId));

        assertThatThrownBy(() -> auditRecordRepository.saveAndFlush(auditRecord(null, eventId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateAuditId() {
        UUID auditId = UUID.randomUUID();

        auditRecordRepository.saveAndFlush(auditRecord(auditId, UUID.randomUUID()));

        assertThatThrownBy(() -> auditRecordRepository.saveAndFlush(
                auditRecord(auditId, UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AuditRecordEntity auditRecord(UUID auditId, UUID eventId) {
        return new AuditRecordEntity(
                auditId,
                eventId,
                "TASK_CREATED",
                "TASK",
                UUID.randomUUID(),
                "task-service",
                null,
                null,
                "CREATE",
                "{}",
                OffsetDateTime.parse("2026-07-02T08:30:00Z")
        );
    }
}

package com.example.ai_service;

import com.example.ai_service.enumeration.OutboxEventStatus;
import com.example.ai_service.outbox.OutboxEventService;
import com.example.ai_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "server.port=0",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "jwt.secret=QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ=="
})
@Testcontainers
class AiDatabaseMigrationTest {

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("ai_service_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextStartsWithPostgresAndFlyway() {
        // Spring context startup verifies DataSource, Flyway, and JPA mappings.
    }

    @Test
    void persistsNewAiOutboxEvent() {
        UUID operationId = UUID.randomUUID();

        outboxEventService.saveNewEvent(
                "AI_OPERATION",
                operationId,
                "AI_TASK_SUMMARIZED",
                "{\"operationType\":\"AI_TASK_SUMMARIZED\"}"
        );

        var event = outboxEventRepository.findAll().stream()
                .filter(candidate -> operationId.equals(candidate.getAggregateId()))
                .findFirst()
                .orElseThrow();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(event.getEventType()).isEqualTo("AI_TASK_SUMMARIZED");
        assertThat(event.getPayload()).doesNotContain("description", "prompt", "generated");
    }
}

package com.example.task_service.usecase;

import com.example.task_service.dto.CreateTaskRequest;
import com.example.task_service.dto.CreateTaskResponse;
import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import com.example.task_service.enumeration.TaskPriority;
import com.example.task_service.repository.OutboxEventRepository;
import com.example.task_service.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.config.import=optional:configserver:",
    "eureka.client.enabled=false",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:create_task_outbox_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CreateTaskOutboxIntegrationTest {

    @Autowired
    private CreateTaskUseCase createTaskUseCase;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsTaskAndTaskCreatedOutboxEventTogether() throws Exception {
        UUID createdByUserId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();

        CreateTaskResponse response = createTaskUseCase.create(new CreateTaskRequest(
            "Task with outbox event",
            "Persist task and TASK_CREATED event.",
            TaskPriority.HIGH,
            assigneeUserId
        ), createdByUserId);

        assertThat(taskRepository.findByTaskId(response.taskId())).isPresent();

        OutboxEventEntity event = outboxEventRepository.findAll().stream()
            .filter(outboxEvent -> response.taskId().equals(outboxEvent.getAggregateId()))
            .findFirst()
            .orElseThrow();

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo("TASK");
        assertThat(event.getAggregateId()).isEqualTo(response.taskId());
        assertThat(event.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getUpdatedAt()).isNotNull();

        JsonNode payload = readPayload(event.getPayload());
        assertThat(payload.get("taskId").asText()).isEqualTo(response.taskId().toString());
        assertThat(payload.get("title").asText()).isEqualTo("Task with outbox event");
        assertThat(payload.get("description").asText()).isEqualTo("Persist task and TASK_CREATED event.");
        assertThat(payload.get("status").asText()).isEqualTo("NEW");
        assertThat(payload.get("priority").asText()).isEqualTo("HIGH");
        assertThat(payload.get("assigneeUserId").asText()).isEqualTo(assigneeUserId.toString());
        assertThat(payload.get("createdByUserId").asText()).isEqualTo(createdByUserId.toString());
        assertThat(payload.get("createdAt").asText()).isNotBlank();
    }

    private JsonNode readPayload(String payload) throws Exception {
        JsonNode json = objectMapper.readTree(payload);
        return json.isTextual() ? objectMapper.readTree(json.asText()) : json;
    }
}

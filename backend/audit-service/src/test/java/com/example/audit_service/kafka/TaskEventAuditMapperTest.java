package com.example.audit_service.kafka;

import com.example.audit_service.usecase.CreateAuditRecordCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskEventAuditMapperTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-07-02T08:30:00Z");

    private TaskEventAuditMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TaskEventAuditMapper(new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void mapsTaskCreatedAndUsesCreatorAsActor() {
        String payload = "{\"taskId\":\"%s\",\"createdByUserId\":\"%s\"}"
                .formatted(TASK_ID, USER_ID);

        CreateAuditRecordCommand command = mapper.toCommand(event("TASK_CREATED", payload));

        assertThat(command.eventId()).isEqualTo(EVENT_ID);
        assertThat(command.eventType()).isEqualTo("TASK_CREATED");
        assertThat(command.aggregateType()).isEqualTo("TASK");
        assertThat(command.aggregateId()).isEqualTo(TASK_ID);
        assertThat(command.sourceService()).isEqualTo("task-service");
        assertThat(command.actorUserId()).isEqualTo(USER_ID);
        assertThat(command.actorEmail()).isNull();
        assertThat(command.action()).isEqualTo("CREATE");
        assertThat(command.payload()).isSameAs(payload);
        assertThat(command.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void doesNotTreatTaskCreatorAsAssignmentActor() {
        String payload = "{\"taskId\":\"%s\",\"createdByUserId\":\"%s\"}"
                .formatted(TASK_ID, USER_ID);

        CreateAuditRecordCommand command = mapper.toCommand(event("TASK_ASSIGNED", payload));

        assertThat(command.actorUserId()).isNull();
        assertThat(command.action()).isEqualTo("ASSIGN");
    }

    @Test
    void mapsExplicitActorInformationWhenPresent() {
        String payload = "{\"actorUserId\":\"%s\",\"actorEmail\":\"actor@example.com\"}"
                .formatted(USER_ID);

        CreateAuditRecordCommand command = mapper.toCommand(event("TASK_STATUS_CHANGED", payload));

        assertThat(command.actorUserId()).isEqualTo(USER_ID);
        assertThat(command.actorEmail()).isEqualTo("actor@example.com");
        assertThat(command.action()).isEqualTo("STATUS_CHANGE");
    }

    @Test
    void rejectsUnsupportedTaskEventType() {
        assertThatThrownBy(() -> mapper.toCommand(event("TASK_DELETED", "{}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported task event type: TASK_DELETED");
    }

    private KafkaOutboxEventMessage event(String eventType, String payload) {
        return new KafkaOutboxEventMessage(
                EVENT_ID,
                eventType,
                "TASK",
                TASK_ID,
                OCCURRED_AT,
                1,
                payload
        );
    }
}

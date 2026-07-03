package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TaskAuditEventNormalizerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-07-02T08:30:00Z");

    private TaskAuditEventNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TaskAuditEventNormalizer(new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void normalizesTaskCreatedAndUsesCreatorAsActor() {
        String payload = "{\"taskId\":\"%s\",\"createdByUserId\":\"%s\"}"
                .formatted(TASK_ID, USER_ID);

        NormalizedAuditEvent normalizedEvent = normalizer
                .normalize(event("TASK_CREATED", payload))
                .orElseThrow();

        assertThat(normalizedEvent.eventId()).isEqualTo(EVENT_ID);
        assertThat(normalizedEvent.eventType()).isEqualTo("TASK_CREATED");
        assertThat(normalizedEvent.aggregateType()).isEqualTo("TASK");
        assertThat(normalizedEvent.aggregateId()).isEqualTo(TASK_ID);
        assertThat(normalizedEvent.sourceService()).isEqualTo("task-service");
        assertThat(normalizedEvent.actorUserId()).isEqualTo(USER_ID);
        assertThat(normalizedEvent.actorEmail()).isNull();
        assertThat(normalizedEvent.action()).isEqualTo("CREATE_TASK");
        assertThat(normalizedEvent.payload()).isSameAs(payload);
        assertThat(normalizedEvent.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void normalizesTaskAssignedWithoutInventingActor() {
        String payload = "{\"taskId\":\"%s\",\"createdByUserId\":\"%s\"}"
                .formatted(TASK_ID, USER_ID);

        NormalizedAuditEvent normalizedEvent = normalizer
                .normalize(event("TASK_ASSIGNED", payload))
                .orElseThrow();

        assertThat(normalizedEvent.actorUserId()).isNull();
        assertThat(normalizedEvent.action()).isEqualTo("ASSIGN_TASK");
    }

    @Test
    void normalizesTaskStatusChangedAndExplicitActorInformation() {
        String payload = "{\"actorUserId\":\"%s\",\"actorEmail\":\"actor@example.com\"}"
                .formatted(USER_ID);

        NormalizedAuditEvent normalizedEvent = normalizer
                .normalize(event("TASK_STATUS_CHANGED", payload))
                .orElseThrow();

        assertThat(normalizedEvent.actorUserId()).isEqualTo(USER_ID);
        assertThat(normalizedEvent.actorEmail()).isEqualTo("actor@example.com");
        assertThat(normalizedEvent.action()).isEqualTo("CHANGE_TASK_STATUS");
    }

    @Test
    void normalizesTaskUpdated() {
        String payload = "{\"actorUserId\":\"%s\"}".formatted(USER_ID);

        NormalizedAuditEvent normalizedEvent = normalizer
                .normalize(event("TASK_UPDATED", payload))
                .orElseThrow();

        assertThat(normalizedEvent.actorUserId()).isEqualTo(USER_ID);
        assertThat(normalizedEvent.action()).isEqualTo("UPDATE_TASK");
    }

    @Test
    void normalizesTaskDeletedAndUsesDeletingUserAsActor() {
        String payload = "{\"deletedByUserId\":\"%s\"}".formatted(USER_ID);

        NormalizedAuditEvent normalizedEvent = normalizer
                .normalize(event("TASK_DELETED", payload))
                .orElseThrow();

        assertThat(normalizedEvent.actorUserId()).isEqualTo(USER_ID);
        assertThat(normalizedEvent.action()).isEqualTo("DELETE_TASK");
    }

    @Test
    void logsAndIgnoresUnsupportedTaskEventType(CapturedOutput output) {
        Optional<NormalizedAuditEvent> normalizedEvent =
                normalizer.normalize(event("TASK_ARCHIVED", "{}"));

        assertThat(normalizedEvent).isEmpty();
        assertThat(output)
                .contains("Ignoring unsupported task audit event")
                .contains("TASK_ARCHIVED")
                .contains(EVENT_ID.toString());
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

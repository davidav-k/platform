package com.example.audit_service.kafka;

import com.example.audit_service.usecase.CreateAuditRecordCommand;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TaskEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    private ObjectMapper objectMapper;
    private TaskEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new TaskEventConsumer(
                objectMapper,
                new TaskEventAuditMapper(objectMapper),
                createAuditRecordUseCase
        );
    }

    @Test
    void delegatesValidTaskEventToPersistenceUseCase() throws Exception {
        when(createAuditRecordUseCase.create(any())).thenReturn(true);

        consumer.consume(message());

        ArgumentCaptor<CreateAuditRecordCommand> captor =
                ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(createAuditRecordUseCase).create(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().aggregateId()).isEqualTo(TASK_ID);
        assertThat(captor.getValue().sourceService()).isEqualTo("task-service");
    }

    @Test
    void logsAndIgnoresDuplicateTaskEvent(CapturedOutput output) throws Exception {
        when(createAuditRecordUseCase.create(any())).thenReturn(false);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(any());
        assertThat(output)
                .contains("Ignoring duplicate audit event")
                .contains(EVENT_ID.toString());
    }

    @Test
    void rejectsAndLogsMalformedEnvelope(CapturedOutput output) {
        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka task audit event envelope is not valid");

        assertThatThrownBy(() -> consumer.consume("null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka task audit event envelope is not valid");

        verify(createAuditRecordUseCase, never()).create(any());
        assertThat(output).contains("Kafka task audit event envelope is not valid");
    }

    private String message() throws Exception {
        return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
                EVENT_ID,
                "TASK_CREATED",
                "TASK",
                TASK_ID,
                OffsetDateTime.parse("2026-07-02T08:30:00Z"),
                1,
                "{\"taskId\":\"%s\"}".formatted(TASK_ID)
        ));
    }
}

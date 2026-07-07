package com.example.audit_service.kafka;

import com.example.audit_service.normalization.AiAuditEventNormalizer;
import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAuditEventConsumerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OPERATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private AiAuditEventNormalizer aiAuditEventNormalizer;

    @Mock
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    private ObjectMapper objectMapper;
    private AiAuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new AiAuditEventConsumer(
                objectMapper,
                aiAuditEventNormalizer,
                createAuditRecordUseCase
        );
    }

    @Test
    void delegatesValidAiEventToAuditPersistence() throws Exception {
        NormalizedAuditEvent normalized = normalizedEvent();
        when(aiAuditEventNormalizer.normalize(any())).thenReturn(Optional.of(normalized));
        when(createAuditRecordUseCase.create(normalized)).thenReturn(true);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(normalized);
    }

    @Test
    void ignoresEventRejectedByNormalizer() throws Exception {
        when(aiAuditEventNormalizer.normalize(any())).thenReturn(Optional.empty());

        consumer.consume(message());

        verify(createAuditRecordUseCase, never()).create(any());
    }

    @Test
    void rejectsMalformedEnvelope() {
        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka AI audit event envelope is not valid");

        verify(aiAuditEventNormalizer, never()).normalize(any());
        verify(createAuditRecordUseCase, never()).create(any());
    }

    private String message() throws Exception {
        return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
                EVENT_ID,
                "AI_TASK_SUMMARIZED",
                "AI_OPERATION",
                OPERATION_ID,
                OffsetDateTime.parse("2026-07-06T10:15:30Z"),
                1,
                "{\"operationType\":\"AI_TASK_SUMMARIZED\"}"
        ));
    }

    private NormalizedAuditEvent normalizedEvent() {
        return new NormalizedAuditEvent(
                EVENT_ID,
                "AI_TASK_SUMMARIZED",
                "AI_OPERATION",
                OPERATION_ID,
                "ai-service",
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "user@example.com",
                "SUMMARIZE_TASK",
                "{\"operationType\":\"AI_TASK_SUMMARIZED\"}",
                OffsetDateTime.parse("2026-07-06T10:15:30Z")
        );
    }
}

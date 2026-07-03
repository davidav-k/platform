package com.example.audit_service.kafka;

import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.normalization.UserAuditEventNormalizer;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class UserEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    @Mock
    private UserAuditEventNormalizer userAuditEventNormalizer;

    private ObjectMapper objectMapper;
    private UserEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new UserEventConsumer(
                objectMapper,
                userAuditEventNormalizer,
                createAuditRecordUseCase
        );
    }

    @Test
    void delegatesValidUserEventToPersistenceUseCase() throws Exception {
        when(userAuditEventNormalizer.normalize(any())).thenReturn(Optional.of(normalizedEvent()));
        when(createAuditRecordUseCase.create(any())).thenReturn(true);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(normalizedEvent());
    }

    @Test
    void logsAndIgnoresDuplicateUserEvent(CapturedOutput output) throws Exception {
        when(userAuditEventNormalizer.normalize(any())).thenReturn(Optional.of(normalizedEvent()));
        when(createAuditRecordUseCase.create(any())).thenReturn(false);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(any());
        assertThat(output)
                .contains("Ignoring duplicate audit event")
                .contains(EVENT_ID.toString());
    }

    @Test
    void ignoresEventRejectedByNormalizer() throws Exception {
        when(userAuditEventNormalizer.normalize(any())).thenReturn(Optional.empty());

        consumer.consume(message());

        verify(createAuditRecordUseCase, never()).create(any());
    }

    @Test
    void rejectsAndLogsMalformedEnvelope(CapturedOutput output) {
        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka user audit event envelope is not valid");

        assertThatThrownBy(() -> consumer.consume("null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka user audit event envelope is not valid");

        verify(createAuditRecordUseCase, never()).create(any());
        verify(userAuditEventNormalizer, never()).normalize(any());
        assertThat(output).contains("Kafka user audit event envelope is not valid");
    }

    private String message() throws Exception {
        return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
                EVENT_ID,
                "USER_REGISTERED",
                "USER",
                USER_ID,
                OffsetDateTime.parse("2026-07-03T08:30:00Z"),
                1,
                "{\"userId\":\"%s\"}".formatted(USER_ID)
        ));
    }

    private NormalizedAuditEvent normalizedEvent() {
        return new NormalizedAuditEvent(
                EVENT_ID,
                "USER_REGISTERED",
                "USER",
                USER_ID,
                "user-service",
                USER_ID,
                "user@example.com",
                "REGISTER_USER",
                "{\"userId\":\"%s\"}".formatted(USER_ID),
                OffsetDateTime.parse("2026-07-03T08:30:00Z")
        );
    }
}

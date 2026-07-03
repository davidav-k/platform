package com.example.audit_service.kafka;

import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.normalization.NotificationAuditEventNormalizer;
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
class NotificationAuditEventConsumerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID NOTIFICATION_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock
    private CreateAuditRecordUseCase createAuditRecordUseCase;

    @Mock
    private NotificationAuditEventNormalizer notificationAuditEventNormalizer;

    private ObjectMapper objectMapper;
    private NotificationAuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new NotificationAuditEventConsumer(
                objectMapper,
                notificationAuditEventNormalizer,
                createAuditRecordUseCase
        );
    }

    @Test
    void delegatesValidNotificationEventToPersistenceUseCase() throws Exception {
        when(notificationAuditEventNormalizer.normalize(any()))
                .thenReturn(Optional.of(normalizedEvent()));
        when(createAuditRecordUseCase.create(any())).thenReturn(true);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(normalizedEvent());
    }

    @Test
    void logsAndIgnoresDuplicateNotificationEvent(CapturedOutput output) throws Exception {
        when(notificationAuditEventNormalizer.normalize(any()))
                .thenReturn(Optional.of(normalizedEvent()));
        when(createAuditRecordUseCase.create(any())).thenReturn(false);

        consumer.consume(message());

        verify(createAuditRecordUseCase).create(any());
        assertThat(output)
                .contains("Ignoring duplicate audit event")
                .contains(EVENT_ID.toString());
    }

    @Test
    void ignoresEventRejectedByNormalizer() throws Exception {
        when(notificationAuditEventNormalizer.normalize(any()))
                .thenReturn(Optional.empty());

        consumer.consume(message());

        verify(createAuditRecordUseCase, never()).create(any());
    }

    @Test
    void rejectsAndLogsMalformedEnvelope(CapturedOutput output) {
        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka notification audit event envelope is not valid");

        assertThatThrownBy(() -> consumer.consume("null"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kafka notification audit event envelope is not valid");

        verify(createAuditRecordUseCase, never()).create(any());
        verify(notificationAuditEventNormalizer, never()).normalize(any());
        assertThat(output).contains("Kafka notification audit event envelope is not valid");
    }

    private String message() throws Exception {
        return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
                EVENT_ID,
                "NOTIFICATION_CREATED",
                "NOTIFICATION",
                NOTIFICATION_ID,
                OffsetDateTime.parse("2026-07-03T08:30:00Z"),
                1,
                "{\"notificationId\":\"%s\"}".formatted(NOTIFICATION_ID)
        ));
    }

    private NormalizedAuditEvent normalizedEvent() {
        return new NormalizedAuditEvent(
                EVENT_ID,
                "NOTIFICATION_CREATED",
                "NOTIFICATION",
                NOTIFICATION_ID,
                "notification-service",
                null,
                null,
                "CREATE_NOTIFICATION",
                "{\"notificationId\":\"%s\"}".formatted(NOTIFICATION_ID),
                OffsetDateTime.parse("2026-07-03T08:30:00Z")
        );
    }
}

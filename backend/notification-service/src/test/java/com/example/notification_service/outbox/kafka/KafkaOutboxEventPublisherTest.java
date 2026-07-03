package com.example.notification_service.outbox.kafka;

import com.example.notification_service.entity.OutboxEventEntity;
import com.example.notification_service.enumeration.OutboxEventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOutboxEventPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private KafkaOutboxEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        KafkaOutboxPublisherProperties properties = new KafkaOutboxPublisherProperties();
        properties.setTopic("platform.notification-events");
        publisher = new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Test
    void publishesEnvelopeToConfiguredNotificationEventsTopic() throws Exception {
        OutboxEventEntity event = event();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("platform.notification-events"),
                eq(event.getAggregateId().toString()),
                valueCaptor.capture()
        );

        JsonNode envelope = objectMapper.readTree(valueCaptor.getValue());
        assertThat(envelope.get("eventId").asText()).isEqualTo(event.getEventId().toString());
        assertThat(envelope.get("eventType").asText()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("NOTIFICATION");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(event.getAggregateId().toString());
        assertThat(envelope.get("occurredAt").asText()).isEqualTo("2026-07-03T08:30:00Z");
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("payload").asText()).isEqualTo(event.getPayload());
    }

    private OutboxEventEntity event() {
        OutboxEventEntity event = new OutboxEventEntity(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "NOTIFICATION",
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "NOTIFICATION_CREATED",
                "{\"status\":\"PENDING\"}",
                OutboxEventStatus.NEW
        );
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-07-03T08:30:00Z"));
        return event;
    }
}

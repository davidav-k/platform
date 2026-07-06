package com.example.ai_service.outbox.kafka;

import com.example.ai_service.entity.OutboxEventEntity;
import com.example.ai_service.enumeration.OutboxEventStatus;
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
        properties.setTopic("platform.ai-events");
        publisher = new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Test
    void publishesPlatformOutboxEnvelopeToConfiguredAiTopic() throws Exception {
        OutboxEventEntity event = event();
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("platform.ai-events"),
                eq(event.getAggregateId().toString()),
                valueCaptor.capture()
        );

        JsonNode envelope = objectMapper.readTree(valueCaptor.getValue());
        assertThat(envelope.get("eventId").asText()).isEqualTo(event.getEventId().toString());
        assertThat(envelope.get("eventType").asText()).isEqualTo("AI_TASK_SUMMARIZED");
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("AI_OPERATION");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(event.getAggregateId().toString());
        assertThat(envelope.get("occurredAt").asText()).isEqualTo("2026-07-06T10:15:30Z");
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("payload").asText()).isEqualTo(event.getPayload());
    }

    private OutboxEventEntity event() {
        OutboxEventEntity event = new OutboxEventEntity(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "AI_OPERATION",
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "AI_TASK_SUMMARIZED",
                "{\"operationType\":\"AI_TASK_SUMMARIZED\"}",
                OutboxEventStatus.NEW
        );
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-07-06T10:15:30Z"));
        return event;
    }
}

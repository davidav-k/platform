package com.example.task_service.outbox.kafka;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOutboxEventPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaOutboxPublisherProperties properties;
    private ObjectMapper objectMapper;
    private KafkaOutboxEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        properties = new KafkaOutboxPublisherProperties();
        properties.setTopic("platform.task-events");
        objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        publisher = new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Test
    void sendsOutboxEventToConfiguredTopicWithAggregateIdKey() throws Exception {
        OutboxEventEntity event = event();
        when(kafkaTemplate.send(
            "platform.task-events",
            event.getAggregateId().toString(),
            expectedSerializedValue()
        )).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        verify(kafkaTemplate).send(
            "platform.task-events",
            event.getAggregateId().toString(),
            expectedSerializedValue()
        );
    }

    @Test
    void serializesKafkaEnvelopeFromOutboxEvent() throws Exception {
        OutboxEventEntity event = event();
        when(kafkaTemplate.send(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
            org.mockito.ArgumentMatchers.eq("platform.task-events"),
            org.mockito.ArgumentMatchers.eq(event.getAggregateId().toString()),
            valueCaptor.capture()
        );

        JsonNode value = objectMapper.readTree(valueCaptor.getValue());
        assertThat(value.get("eventId").asText()).isEqualTo(event.getEventId().toString());
        assertThat(value.get("eventType").asText()).isEqualTo("TASK_CREATED");
        assertThat(value.get("aggregateType").asText()).isEqualTo("TASK");
        assertThat(value.get("aggregateId").asText()).isEqualTo(event.getAggregateId().toString());
        assertThat(value.get("occurredAt").asText()).isEqualTo("2026-06-23T10:15:30Z");
        assertThat(value.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(value.get("payload").asText()).isEqualTo(event.getPayload());
    }

    @Test
    void propagatesKafkaSendFailure() {
        OutboxEventEntity event = event();
        when(kafkaTemplate.send(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        assertThatThrownBy(() -> publisher.publish(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to publish outbox event to Kafka")
            .hasRootCauseMessage("broker unavailable");
    }

    private OutboxEventEntity event() {
        UUID eventId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID aggregateId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        OutboxEventEntity event = new OutboxEventEntity(
            eventId,
            "TASK",
            aggregateId,
            "TASK_CREATED",
            "{\"taskId\":\"" + aggregateId + "\"}",
            OutboxEventStatus.NEW
        );
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-06-23T10:15:30Z"));
        return event;
    }

    private String expectedSerializedValue() throws Exception {
        return objectMapper.writeValueAsString(new KafkaOutboxEventMessage(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "TASK_CREATED",
            "TASK",
            UUID.fromString("20000000-0000-0000-0000-000000000002"),
            OffsetDateTime.parse("2026-06-23T10:15:30Z"),
            1,
            "{\"taskId\":\"20000000-0000-0000-0000-000000000002\"}"
        ));
    }
}

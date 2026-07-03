package com.example.user_service.outbox.kafka;

import com.example.user_service.entity.OutboxEventEntity;
import com.example.user_service.enumeration.OutboxEventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaOutboxPublisherProperties properties = new KafkaOutboxPublisherProperties();
        properties.setTopic("platform.user-events");
        publisher = new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
    }

    @Test
    void publishesEnvelopeToConfiguredUserEventsTopic() throws Exception {
        UUID eventId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID aggregateId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        OutboxEventEntity event = new OutboxEventEntity(
                eventId,
                "USER",
                aggregateId,
                "USER_REGISTERED",
                "{\"userId\":\"" + aggregateId + "\"}",
                OutboxEventStatus.NEW
        );
        ReflectionTestUtils.setField(
                event, "createdAt", OffsetDateTime.parse("2026-07-03T08:30:00Z"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("platform.user-events"),
                eq(aggregateId.toString()),
                valueCaptor.capture()
        );
        JsonNode envelope = objectMapper.readTree(valueCaptor.getValue());
        assertThat(envelope.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(envelope.get("eventType").asText()).isEqualTo("USER_REGISTERED");
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("USER");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(aggregateId.toString());
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("payload").asText()).isEqualTo(event.getPayload());
    }
}

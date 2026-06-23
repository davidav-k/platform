package com.example.task_service.outbox.kafka;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.outbox.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "outbox.publisher", name = "adapter", havingValue = "kafka")
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private static final int EVENT_VERSION = 1;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaOutboxPublisherProperties properties;

    public KafkaOutboxEventPublisher(
        @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper,
        KafkaOutboxPublisherProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void publish(OutboxEventEntity event) {
        String key = event.getAggregateId().toString();
        String value = serialize(message(event));
        try {
            kafkaTemplate.send(properties.getTopic(), key, value).get();
            log.info(
                "Published outbox event to Kafka: eventId={}, eventType={}, aggregateType={}, aggregateId={}, topic={}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                properties.getTopic()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event to Kafka", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to publish outbox event to Kafka", exception.getCause());
        }
    }

    private KafkaOutboxEventMessage message(OutboxEventEntity event) {
        return new KafkaOutboxEventMessage(
            event.getEventId(),
            event.getEventType(),
            event.getAggregateType(),
            event.getAggregateId(),
            event.getCreatedAt(),
            EVENT_VERSION,
            event.getPayload()
        );
    }

    private String serialize(KafkaOutboxEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox event for Kafka", exception);
        }
    }
}

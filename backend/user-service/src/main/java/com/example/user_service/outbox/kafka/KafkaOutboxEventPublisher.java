package com.example.user_service.outbox.kafka;

import com.example.user_service.entity.OutboxEventEntity;
import com.example.user_service.outbox.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox.publisher", name = "adapter", havingValue = "kafka")
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private static final int EVENT_VERSION = 1;

    private final KafkaTemplate<String, String> userOutboxKafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaOutboxPublisherProperties properties;

    @Override
    public void publish(OutboxEventEntity event) {
        String key = event.getAggregateId().toString();
        String value = serialize(message(event));
        try {
            userOutboxKafkaTemplate.send(properties.getTopic(), key, value).get();
            log.info(
                    "Published user outbox event to Kafka: eventId={}, eventType={}, aggregateType={}, aggregateId={}, topic={}",
                    event.getEventId(), event.getEventType(), event.getAggregateType(),
                    event.getAggregateId(), properties.getTopic()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing user outbox event to Kafka", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to publish user outbox event to Kafka", exception.getCause());
        }
    }

    private KafkaOutboxEventMessage message(OutboxEventEntity event) {
        return new KafkaOutboxEventMessage(
                event.getEventId(), event.getEventType(), event.getAggregateType(),
                event.getAggregateId(), event.getCreatedAt(), EVENT_VERSION, event.getPayload()
        );
    }

    private String serialize(KafkaOutboxEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user outbox event for Kafka", exception);
        }
    }
}

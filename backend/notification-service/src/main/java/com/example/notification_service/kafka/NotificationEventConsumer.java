package com.example.notification_service.kafka;

import com.example.notification_service.entity.ConsumedEventEntity;
import com.example.notification_service.repository.ConsumedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true")
public class NotificationEventConsumer {

    private static final String SOURCE = "task-service";

    private final ConsumedEventRepository consumedEventRepository;
    private final ObjectMapper objectMapper;
    private final TaskEventNotificationProcessor taskEventNotificationProcessor;

    @KafkaListener(
        topics = "${notification.kafka.topic:platform.task-events}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    @Transactional
    public void consume(String message) {
        KafkaOutboxEventMessage event = deserialize(message);

        if (consumedEventRepository.existsByEventId(event.eventId())) {
            log.info("Ignoring duplicate notification event: eventId={}, eventType={}",
                event.eventId(), event.eventType());
            return;
        }

        taskEventNotificationProcessor.process(event);
        consumedEventRepository.saveAndFlush(new ConsumedEventEntity(
            event.eventId(),
            event.eventType(),
            SOURCE
        ));
        log.info("Accepted notification event: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
            event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId());
    }

    private KafkaOutboxEventMessage deserialize(String message) {
        try {
            return objectMapper.readValue(message, KafkaOutboxEventMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka notification event payload is not valid", exception);
        }
    }
}

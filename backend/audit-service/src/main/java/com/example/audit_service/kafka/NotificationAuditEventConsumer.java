package com.example.audit_service.kafka;

import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.normalization.NotificationAuditEventNormalizer;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
public class NotificationAuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationAuditEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationAuditEventNormalizer notificationAuditEventNormalizer;
    private final CreateAuditRecordUseCase createAuditRecordUseCase;

    public NotificationAuditEventConsumer(
            ObjectMapper objectMapper,
            NotificationAuditEventNormalizer notificationAuditEventNormalizer,
            CreateAuditRecordUseCase createAuditRecordUseCase
    ) {
        this.objectMapper = objectMapper;
        this.notificationAuditEventNormalizer = notificationAuditEventNormalizer;
        this.createAuditRecordUseCase = createAuditRecordUseCase;
    }

    @KafkaListener(
            topics = "${audit.kafka.notification-topic:platform.notification-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        KafkaOutboxEventMessage event = deserialize(message);
        Optional<NormalizedAuditEvent> normalizedEvent =
                notificationAuditEventNormalizer.normalize(event);
        if (normalizedEvent.isEmpty()) {
            return;
        }

        boolean created = createAuditRecordUseCase.create(normalizedEvent.get());
        if (!created) {
            log.info("Ignoring duplicate audit event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
            return;
        }

        log.info(
                "Accepted notification audit event: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId()
        );
    }

    private KafkaOutboxEventMessage deserialize(String message) {
        try {
            KafkaOutboxEventMessage event =
                    objectMapper.readValue(message, KafkaOutboxEventMessage.class);
            if (event == null) {
                log.error("Kafka notification audit event envelope is not valid");
                throw new IllegalArgumentException(
                        "Kafka notification audit event envelope is not valid"
                );
            }
            return event;
        } catch (JsonProcessingException exception) {
            log.error("Kafka notification audit event envelope is not valid", exception);
            throw new IllegalArgumentException(
                    "Kafka notification audit event envelope is not valid",
                    exception
            );
        }
    }
}

package com.example.audit_service.kafka;

import com.example.audit_service.normalization.AiAuditEventNormalizer;
import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
public class AiAuditEventConsumer {

    private final ObjectMapper objectMapper;
    private final AiAuditEventNormalizer aiAuditEventNormalizer;
    private final CreateAuditRecordUseCase createAuditRecordUseCase;

    @KafkaListener(
            topics = "${audit.kafka.ai-topic:platform.ai-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        KafkaOutboxEventMessage event = deserialize(message);
        Optional<NormalizedAuditEvent> normalizedEvent = aiAuditEventNormalizer.normalize(event);
        if (normalizedEvent.isEmpty()) {
            return;
        }

        boolean created = createAuditRecordUseCase.create(normalizedEvent.get());
        if (!created) {
            log.info("Ignoring duplicate audit event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
            return;
        }

        log.info("Accepted AI audit event: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId());
    }

    private KafkaOutboxEventMessage deserialize(String message) {
        try {
            KafkaOutboxEventMessage event = objectMapper.readValue(message, KafkaOutboxEventMessage.class);
            if (event == null) {
                throw new IllegalArgumentException("Kafka AI audit event envelope is not valid");
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka AI audit event envelope is not valid", exception);
        }
    }
}

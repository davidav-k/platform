package com.example.audit_service.kafka;

import com.example.audit_service.usecase.CreateAuditRecordCommand;
import com.example.audit_service.usecase.CreateAuditRecordUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final TaskEventAuditMapper taskEventAuditMapper;
    private final CreateAuditRecordUseCase createAuditRecordUseCase;

    public TaskEventConsumer(ObjectMapper objectMapper, TaskEventAuditMapper taskEventAuditMapper,
                             CreateAuditRecordUseCase createAuditRecordUseCase) {
        this.objectMapper = objectMapper;
        this.taskEventAuditMapper = taskEventAuditMapper;
        this.createAuditRecordUseCase = createAuditRecordUseCase;
    }

    @KafkaListener(
            topics = "${audit.kafka.topic:platform.task-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}",
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        KafkaOutboxEventMessage event = deserialize(message);
        CreateAuditRecordCommand command = taskEventAuditMapper.toCommand(event);
        boolean created = createAuditRecordUseCase.create(command);

        if (!created) {
            log.info("Ignoring duplicate audit event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
            return;
        }

        log.info("Accepted task audit event: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
                event.eventId(), event.eventType(), event.aggregateType(), event.aggregateId());
    }

    private KafkaOutboxEventMessage deserialize(String message) {
        try {
            KafkaOutboxEventMessage event = objectMapper.readValue(message, KafkaOutboxEventMessage.class);
            if (event == null) {
                log.error("Kafka task audit event envelope is not valid");
                throw new IllegalArgumentException("Kafka task audit event envelope is not valid");
            }
            return event;
        } catch (JsonProcessingException exception) {
            log.error("Kafka task audit event envelope is not valid", exception);
            throw new IllegalArgumentException("Kafka task audit event envelope is not valid", exception);
        }
    }
}

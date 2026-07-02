package com.example.audit_service.kafka;

import com.example.audit_service.usecase.CreateAuditRecordCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaskEventAuditMapper {

    private static final String TASK_CREATED = "TASK_CREATED";
    private static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    private static final String SOURCE_SERVICE = "task-service";

    private final ObjectMapper objectMapper;

    public TaskEventAuditMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CreateAuditRecordCommand toCommand(KafkaOutboxEventMessage event) {
        JsonNode payload = deserializePayload(event.payload());
        return new CreateAuditRecordCommand(
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                SOURCE_SERVICE,
                actorUserId(event.eventType(), payload),
                textOrNull(payload, "actorEmail"),
                action(event.eventType()),
                event.payload(),
                event.occurredAt()
        );
    }

    private String action(String eventType) {
        return switch (eventType) {
            case TASK_CREATED -> "CREATE";
            case TASK_ASSIGNED -> "ASSIGN";
            case TASK_STATUS_CHANGED -> "STATUS_CHANGE";
            default -> throw new IllegalArgumentException("Unsupported task event type: " + eventType);
        };
    }

    private UUID actorUserId(String eventType, JsonNode payload) {
        UUID actorUserId = uuidOrNull(payload, "actorUserId");
        if (actorUserId == null && TASK_CREATED.equals(eventType)) {
            return uuidOrNull(payload, "createdByUserId");
        }
        return actorUserId;
    }

    private JsonNode deserializePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Kafka task event payload is not valid");
        }
        try {
            JsonNode parsedPayload = objectMapper.readTree(payload);
            if (parsedPayload == null) {
                throw new IllegalArgumentException("Kafka task event payload is not valid");
            }
            return parsedPayload;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka task event payload is not valid", exception);
        }
    }

    private UUID uuidOrNull(JsonNode payload, String fieldName) {
        String value = textOrNull(payload, fieldName);
        return value == null ? null : UUID.fromString(value);
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual() || value.textValue().isBlank()) {
            return null;
        }
        return value.textValue();
    }
}

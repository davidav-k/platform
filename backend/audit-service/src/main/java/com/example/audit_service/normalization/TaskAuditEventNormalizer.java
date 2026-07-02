package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TaskAuditEventNormalizer {

    private static final Logger log = LoggerFactory.getLogger(TaskAuditEventNormalizer.class);

    private static final String TASK_CREATED = "TASK_CREATED";
    private static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    private static final String TASK_UPDATED = "TASK_UPDATED";
    private static final String TASK_DELETED = "TASK_DELETED";
    private static final String SOURCE_SERVICE = "task-service";

    private final ObjectMapper objectMapper;

    public TaskAuditEventNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<NormalizedAuditEvent> normalize(KafkaOutboxEventMessage event) {
        String action = actionOrNull(event.eventType());
        if (action == null) {
            log.warn("Ignoring unsupported task audit event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
            return Optional.empty();
        }

        JsonNode payload = deserializePayload(event.payload());
        return Optional.of(new NormalizedAuditEvent(
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                SOURCE_SERVICE,
                actorUserId(event.eventType(), payload),
                textOrNull(payload, "actorEmail"),
                action,
                event.payload(),
                event.occurredAt()
        ));
    }

    private String actionOrNull(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case TASK_CREATED -> "CREATE_TASK";
            case TASK_ASSIGNED -> "ASSIGN_TASK";
            case TASK_STATUS_CHANGED -> "CHANGE_TASK_STATUS";
            case TASK_UPDATED -> "UPDATE_TASK";
            case TASK_DELETED -> "DELETE_TASK";
            default -> null;
        };
    }

    private UUID actorUserId(String eventType, JsonNode payload) {
        UUID actorUserId = uuidOrNull(payload, "actorUserId");
        if (actorUserId == null && TASK_CREATED.equals(eventType)) {
            return uuidOrNull(payload, "createdByUserId");
        }
        if (actorUserId == null && TASK_DELETED.equals(eventType)) {
            return uuidOrNull(payload, "deletedByUserId");
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

package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiAuditEventNormalizer {

    private static final String AI_TASK_DESCRIPTION_IMPROVED = "AI_TASK_DESCRIPTION_IMPROVED";
    private static final String AI_SUBTASKS_SUGGESTED = "AI_SUBTASKS_SUGGESTED";
    private static final String AI_TASK_SUMMARIZED = "AI_TASK_SUMMARIZED";
    private static final String AI_PRIORITY_SUGGESTED = "AI_PRIORITY_SUGGESTED";
    private static final String SOURCE_SERVICE = "ai-service";
    private static final List<String> ALLOWED_PAYLOAD_FIELDS = List.of(
            "operationId",
            "operationType",
            "actorUserId",
            "actorEmail",
            "occurredAt",
            "providerName",
            "modelName"
    );

    private final ObjectMapper objectMapper;

    public Optional<NormalizedAuditEvent> normalize(KafkaOutboxEventMessage event) {
        String action = actionOrNull(event.eventType());
        if (action == null) {
            log.warn("Ignoring unsupported AI audit event: eventId={}, eventType={}",
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
                uuidOrNull(payload, "actorUserId"),
                textOrNull(payload, "actorEmail"),
                action,
                sanitizePayload(payload),
                event.occurredAt()
        ));
    }

    private String actionOrNull(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case AI_TASK_DESCRIPTION_IMPROVED -> "IMPROVE_TASK_DESCRIPTION";
            case AI_SUBTASKS_SUGGESTED -> "SUGGEST_SUBTASKS";
            case AI_TASK_SUMMARIZED -> "SUMMARIZE_TASK";
            case AI_PRIORITY_SUGGESTED -> "SUGGEST_PRIORITY";
            default -> null;
        };
    }

    private JsonNode deserializePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Kafka AI event payload is not valid");
        }
        try {
            JsonNode parsedPayload = objectMapper.readTree(payload);
            if (parsedPayload == null || !parsedPayload.isObject()) {
                throw new IllegalArgumentException("Kafka AI event payload is not valid");
            }
            return parsedPayload;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka AI event payload is not valid", exception);
        }
    }

    private String sanitizePayload(JsonNode payload) {
        ObjectNode sanitized = objectMapper.createObjectNode();
        ALLOWED_PAYLOAD_FIELDS.forEach(fieldName -> {
            JsonNode value = payload.get(fieldName);
            if (value != null) {
                sanitized.set(fieldName, value);
            }
        });
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize sanitized AI audit payload", exception);
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

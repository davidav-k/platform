package com.example.audit_service.normalization;

import com.example.audit_service.kafka.KafkaOutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class NotificationAuditEventNormalizer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationAuditEventNormalizer.class);

    private static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    private static final String NOTIFICATION_SYSTEM_CREATED = "NOTIFICATION_SYSTEM_CREATED";
    private static final String SOURCE_SERVICE = "notification-service";

    private final ObjectMapper objectMapper;

    public NotificationAuditEventNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<NormalizedAuditEvent> normalize(KafkaOutboxEventMessage event) {
        String action = actionOrNull(event.eventType());
        if (action == null) {
            log.warn("Ignoring unsupported notification audit event: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
            return Optional.empty();
        }

        JsonNode payload = deserializePayload(event.payload());
        JsonNode sanitizedPayload = payload.deepCopy();
        removeSensitiveFields(sanitizedPayload);

        return Optional.of(new NormalizedAuditEvent(
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                SOURCE_SERVICE,
                null,
                null,
                action,
                serializePayload(sanitizedPayload),
                event.occurredAt()
        ));
    }

    private String actionOrNull(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType) {
            case NOTIFICATION_CREATED -> "CREATE_NOTIFICATION";
            case NOTIFICATION_SYSTEM_CREATED -> "CREATE_SYSTEM_NOTIFICATION";
            default -> null;
        };
    }

    private JsonNode deserializePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Kafka notification event payload is not valid");
        }
        try {
            JsonNode parsedPayload = objectMapper.readTree(payload);
            if (parsedPayload == null || !parsedPayload.isContainerNode()) {
                throw new IllegalArgumentException(
                        "Kafka notification event payload is not valid"
                );
            }
            return parsedPayload;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Kafka notification event payload is not valid",
                    exception
            );
        }
    }

    private String serializePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize sanitized notification audit payload",
                    exception
            );
        }
    }

    private void removeSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> sensitiveFieldNames = new ArrayList<>();
            objectNode.fields().forEachRemaining(field -> {
                if (isSensitiveField(field.getKey())) {
                    sensitiveFieldNames.add(field.getKey());
                } else {
                    removeSensitiveFields(field.getValue());
                }
            });
            objectNode.remove(sensitiveFieldNames);
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::removeSensitiveFields);
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalizedName = fieldName
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return normalizedName.contains("password")
                || normalizedName.contains("jwt")
                || normalizedName.contains("token")
                || normalizedName.contains("cookie")
                || normalizedName.contains("header")
                || normalizedName.contains("authorization")
                || normalizedName.contains("secret");
    }
}

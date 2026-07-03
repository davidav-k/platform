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
import java.util.UUID;

@Component
public class UserAuditEventNormalizer {

    private static final Logger log = LoggerFactory.getLogger(UserAuditEventNormalizer.class);

    private static final String USER_REGISTERED = "USER_REGISTERED";
    private static final String USER_LOGIN_SUCCESS = "USER_LOGIN_SUCCESS";
    private static final String USER_LOGIN_FAILED = "USER_LOGIN_FAILED";
    private static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    private static final String USER_DELETED = "USER_DELETED";
    private static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String MFA_ENABLED = "MFA_ENABLED";
    private static final String SOURCE_SERVICE = "user-service";

    private final ObjectMapper objectMapper;

    public UserAuditEventNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<NormalizedAuditEvent> normalize(KafkaOutboxEventMessage event) {
        String action = actionOrNull(event.eventType());
        if (action == null) {
            log.warn("Ignoring unsupported user audit event: eventId={}, eventType={}",
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
                actorUserId(event.eventType(), payload),
                textOrNull(payload, "email"),
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
            case USER_REGISTERED -> "REGISTER_USER";
            case USER_LOGIN_SUCCESS -> "LOGIN_SUCCESS";
            case USER_LOGIN_FAILED -> "LOGIN_FAILED";
            case USER_PROFILE_UPDATED -> "UPDATE_USER_PROFILE";
            case USER_DELETED -> "DELETE_USER";
            case PASSWORD_CHANGED -> "CHANGE_PASSWORD";
            case MFA_ENABLED -> "ENABLE_MFA";
            default -> null;
        };
    }

    private UUID actorUserId(String eventType, JsonNode payload) {
        if (USER_DELETED.equals(eventType)) {
            UUID deletedByUserId = uuidOrNull(payload, "deletedByUserId");
            if (deletedByUserId != null) {
                return deletedByUserId;
            }
        }
        return uuidOrNull(payload, "userId");
    }

    private JsonNode deserializePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Kafka user event payload is not valid");
        }
        try {
            JsonNode parsedPayload = objectMapper.readTree(payload);
            if (parsedPayload == null || !parsedPayload.isContainerNode()) {
                throw new IllegalArgumentException("Kafka user event payload is not valid");
            }
            return parsedPayload;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka user event payload is not valid", exception);
        }
    }

    private String serializePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize sanitized user audit payload", exception);
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
                || normalizedName.contains("secret")
                || normalizedName.equals("confirmationkey");
    }

    private UUID uuidOrNull(JsonNode payload, String fieldName) {
        String value = textOrNull(payload, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual() || value.textValue().isBlank()) {
            return null;
        }
        return value.textValue();
    }
}

package com.example.ai_service.outbox;

import com.example.ai_service.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiAuditPayloadFactory {

    private final ObjectMapper objectMapper;

    public String operationCompletedPayload(UUID operationId, String operationType,
                                            AuthenticatedUser actor, OffsetDateTime occurredAt,
                                            String providerName, String modelName) {
        AiOperationAuditPayload payload = new AiOperationAuditPayload(
                operationId,
                operationType,
                actor.userId(),
                occurredAt,
                providerName,
                modelName
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI audit payload", exception);
        }
    }

    private record AiOperationAuditPayload(
            UUID operationId,
            String operationType,
            UUID actorUserId,
            OffsetDateTime occurredAt,
            String providerName,
            String modelName
    ) {
    }
}

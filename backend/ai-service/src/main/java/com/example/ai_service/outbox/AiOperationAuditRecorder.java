package com.example.ai_service.outbox;

import com.example.ai_service.provider.AiProviderMetadataProperties;
import com.example.ai_service.security.AuthenticatedUser;
import com.example.ai_service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiOperationAuditRecorder {

    private static final String AGGREGATE_TYPE = "AI_OPERATION";

    private final CurrentUserProvider currentUserProvider;
    private final AiProviderMetadataProperties providerMetadata;
    private final AiAuditPayloadFactory payloadFactory;
    private final OutboxEventService outboxEventService;

    public void recordSuccess(String eventType) {
        AuthenticatedUser actor = currentUserProvider.currentUser();
        UUID operationId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        String payload = payloadFactory.operationCompletedPayload(
                operationId,
                eventType,
                actor,
                occurredAt,
                providerMetadata.getName(),
                providerMetadata.getModel()
        );
        outboxEventService.saveNewEvent(AGGREGATE_TYPE, operationId, eventType, payload);
    }
}

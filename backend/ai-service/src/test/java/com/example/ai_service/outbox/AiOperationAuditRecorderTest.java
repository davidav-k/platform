package com.example.ai_service.outbox;

import com.example.ai_service.provider.AiProviderMetadataProperties;
import com.example.ai_service.security.AuthenticatedUser;
import com.example.ai_service.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiOperationAuditRecorderTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AiAuditPayloadFactory payloadFactory;

    @Mock
    private OutboxEventService outboxEventService;

    private AiOperationAuditRecorder recorder;

    @BeforeEach
    void setUp() {
        AiProviderMetadataProperties metadata = new AiProviderMetadataProperties();
        metadata.setName("ollama");
        metadata.setModel("llama3.2");
        recorder = new AiOperationAuditRecorder(
                currentUserProvider,
                metadata,
                payloadFactory,
                outboxEventService
        );
    }

    @Test
    void recordsOperationUsingTheGeneratedOperationIdAsAggregateId() {
        AuthenticatedUser actor = new AuthenticatedUser(
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                "user@example.com"
        );
        when(currentUserProvider.currentUser()).thenReturn(actor);
        when(payloadFactory.operationCompletedPayload(
                any(), eq(AiOutboxEventTypes.PRIORITY_SUGGESTED), eq(actor),
                any(), eq("ollama"), eq("llama3.2")
        )).thenReturn("{\"operationType\":\"AI_PRIORITY_SUGGESTED\"}");

        recorder.recordSuccess(AiOutboxEventTypes.PRIORITY_SUGGESTED);

        ArgumentCaptor<UUID> payloadOperationId = ArgumentCaptor.forClass(UUID.class);
        verify(payloadFactory).operationCompletedPayload(
                payloadOperationId.capture(),
                eq(AiOutboxEventTypes.PRIORITY_SUGGESTED),
                eq(actor),
                any(OffsetDateTime.class),
                eq("ollama"),
                eq("llama3.2")
        );
        verify(outboxEventService).saveNewEvent(
                eq("AI_OPERATION"),
                eq(payloadOperationId.getValue()),
                eq(AiOutboxEventTypes.PRIORITY_SUGGESTED),
                eq("{\"operationType\":\"AI_PRIORITY_SUGGESTED\"}")
        );
        assertThat(payloadOperationId.getValue()).isNotNull();
    }
}

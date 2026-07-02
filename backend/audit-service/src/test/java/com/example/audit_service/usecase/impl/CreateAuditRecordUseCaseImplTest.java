package com.example.audit_service.usecase.impl;

import com.example.audit_service.entity.AuditRecordEntity;
import com.example.audit_service.normalization.NormalizedAuditEvent;
import com.example.audit_service.repository.AuditRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAuditRecordUseCaseImplTest {

    @Mock
    private AuditRecordRepository auditRecordRepository;

    private CreateAuditRecordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAuditRecordUseCaseImpl(auditRecordRepository);
    }

    @Test
    void createsNormalizedAuditRecordAndPreservesPayload() {
        NormalizedAuditEvent event = event();
        String payload = event.payload();
        when(auditRecordRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(auditRecordRepository.save(any(AuditRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean created = useCase.create(event);

        assertThat(created).isTrue();
        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRecordRepository).save(captor.capture());
        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(saved.getAggregateType()).isEqualTo("TASK");
        assertThat(saved.getSourceService()).isEqualTo("task-service");
        assertThat(saved.getActorEmail()).isNull();
        assertThat(saved.getAction()).isEqualTo("CREATE_TASK");
        assertThat(saved.getPayload()).isSameAs(payload);
    }

    @Test
    void skipsEventThatWasAlreadyStored() {
        NormalizedAuditEvent event = event();
        when(auditRecordRepository.existsByEventId(event.eventId())).thenReturn(true);

        boolean created = useCase.create(event);

        assertThat(created).isFalse();
        verify(auditRecordRepository, never()).save(any());
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> useCase.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Normalized audit event is required");

        NormalizedAuditEvent missingEventId = new NormalizedAuditEvent(
                null,
                "TASK_CREATED",
                "TASK",
                UUID.randomUUID(),
                "task-service",
                null,
                null,
                "CREATE_TASK",
                "{}",
                OffsetDateTime.now()
        );

        assertThatThrownBy(() -> useCase.create(missingEventId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event ID is required");

        NormalizedAuditEvent blankPayload = new NormalizedAuditEvent(
                UUID.randomUUID(),
                "TASK_CREATED",
                "TASK",
                UUID.randomUUID(),
                "task-service",
                null,
                null,
                "CREATE_TASK",
                "  ",
                OffsetDateTime.now()
        );

        assertThatThrownBy(() -> useCase.create(blankPayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload is required");
    }

    private NormalizedAuditEvent event() {
        return new NormalizedAuditEvent(
                UUID.randomUUID(),
                " TASK_CREATED ",
                " TASK ",
                UUID.randomUUID(),
                " task-service ",
                UUID.randomUUID(),
                "   ",
                " CREATE_TASK ",
                "  {\"title\":\"Keep original whitespace\"}  ",
                OffsetDateTime.parse("2026-07-02T08:30:00Z")
        );
    }
}

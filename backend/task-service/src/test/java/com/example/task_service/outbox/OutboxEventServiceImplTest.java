package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import com.example.task_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OutboxEventServiceImplTest {

    private OutboxEventRepository outboxEventRepository;
    private OutboxEventServiceImpl outboxEventService;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        outboxEventService = new OutboxEventServiceImpl(outboxEventRepository);
    }

    @Test
    void savesNewOutboxEvent() {
        UUID aggregateId = UUID.randomUUID();

        outboxEventService.saveNewEvent(
            " TASK ",
            aggregateId,
            " TASK_CREATED ",
            "{\"taskId\":\"" + aggregateId + "\"}"
        );

        ArgumentCaptor<OutboxEventEntity> eventCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEventEntity saved = eventCaptor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("TASK");
        assertThat(saved.getAggregateId()).isEqualTo(aggregateId);
        assertThat(saved.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(saved.getPayload()).isEqualTo("{\"taskId\":\"" + aggregateId + "\"}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    void rejectsBlankAggregateType() {
        assertThatThrownBy(() -> outboxEventService.saveNewEvent(
            " ",
            UUID.randomUUID(),
            "TASK_CREATED",
            "{}"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Aggregate type is required");

        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsNullAggregateId() {
        assertThatThrownBy(() -> outboxEventService.saveNewEvent(
            "TASK",
            null,
            "TASK_CREATED",
            "{}"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Aggregate ID is required");

        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBlankEventType() {
        assertThatThrownBy(() -> outboxEventService.saveNewEvent(
            "TASK",
            UUID.randomUUID(),
            " ",
            "{}"
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Event type is required");

        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBlankPayload() {
        assertThatThrownBy(() -> outboxEventService.saveNewEvent(
            "TASK",
            UUID.randomUUID(),
            "TASK_CREATED",
            " "
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payload is required");

        verify(outboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import com.example.task_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventProcessorTest {

    private OutboxEventRepository outboxEventRepository;
    private OutboxEventPublisher outboxEventPublisher;
    private OutboxPublisherProperties properties;
    private OutboxEventProcessor processor;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        outboxEventPublisher = mock(OutboxEventPublisher.class);
        properties = new OutboxPublisherProperties();
        processor = new OutboxEventProcessor(
            outboxEventRepository,
            outboxEventPublisher,
            properties,
            new ImmediateTransactionOperations()
        );

        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.saveAllAndFlush(anyCollection()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulPublishChangesStatusToProcessedAndSetsProcessedAt() {
        OutboxEventEntity event = newEvent();
        when(outboxEventRepository.findClaimableEvents(anyCollection(), eq(3), any(Pageable.class)))
            .thenReturn(List.of(event));

        int processed = processor.processBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getErrorMessage()).isNull();
        verify(outboxEventPublisher).publish(event);
        verify(outboxEventRepository).saveAndFlush(event);
    }

    @Test
    void failedPublishChangesStatusToFailedAndStoresRetryState() {
        OutboxEventEntity event = newEvent();
        when(outboxEventRepository.findClaimableEvents(anyCollection(), eq(3), any(Pageable.class)))
            .thenReturn(List.of(event));
        doThrow(new IllegalStateException("publish failed"))
            .when(outboxEventPublisher).publish(event);

        int processed = processor.processBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getProcessedAt()).isNull();
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).isEqualTo("IllegalStateException: publish failed");
        verify(outboxEventPublisher).publish(event);
        verify(outboxEventRepository).saveAndFlush(event);
    }

    @Test
    void claimsNewAndFailedEventsUnderConfiguredBatchAndRetryLimits() {
        properties.setBatchSize(2);
        properties.setMaxRetries(5);
        when(outboxEventRepository.findClaimableEvents(anyCollection(), eq(5), any(Pageable.class)))
            .thenReturn(List.of());

        int processed = processor.processBatch();

        assertThat(processed).isZero();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<OutboxEventStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(outboxEventRepository).findClaimableEvents(statusesCaptor.capture(), eq(5), pageableCaptor.capture());
        assertThat(statusesCaptor.getValue())
            .containsExactlyInAnyOrder(OutboxEventStatus.NEW, OutboxEventStatus.FAILED);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    private OutboxEventEntity newEvent() {
        return new OutboxEventEntity(
            "TASK",
            UUID.randomUUID(),
            "TASK_CREATED",
            "{\"taskId\":\"" + UUID.randomUUID() + "\"}",
            OutboxEventStatus.NEW
        );
    }

    private static class ImmediateTransactionOperations implements TransactionOperations {

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            TransactionStatus status = new SimpleTransactionStatus();
            return action.doInTransaction(status);
        }
    }
}

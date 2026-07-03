package com.example.notification_service.outbox;

import com.example.notification_service.entity.OutboxEventEntity;
import com.example.notification_service.enumeration.OutboxEventStatus;
import com.example.notification_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

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
    private OutboxEventProcessor processor;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        outboxEventPublisher = mock(OutboxEventPublisher.class);
        processor = new OutboxEventProcessor(
                outboxEventRepository,
                outboxEventPublisher,
                new OutboxPublisherProperties(),
                new ImmediateTransactionOperations()
        );
        when(outboxEventRepository.saveAllAndFlush(anyCollection()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void publishesClaimedEventAndMarksItProcessed() {
        OutboxEventEntity event = event();
        when(outboxEventRepository.findClaimableEvents(anyCollection(), eq(3), any(Pageable.class)))
                .thenReturn(List.of(event));

        assertThat(processor.processBatch()).isEqualTo(1);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventPublisher).publish(event);
    }

    @Test
    void marksFailedPublishForRetry() {
        OutboxEventEntity event = event();
        when(outboxEventRepository.findClaimableEvents(anyCollection(), eq(3), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("broker unavailable"))
                .when(outboxEventPublisher).publish(event);

        assertThat(processor.processBatch()).isEqualTo(1);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).isEqualTo("IllegalStateException: broker unavailable");
    }

    private OutboxEventEntity event() {
        return new OutboxEventEntity(
                "NOTIFICATION",
                UUID.randomUUID(),
                "NOTIFICATION_CREATED",
                "{\"status\":\"PENDING\"}",
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

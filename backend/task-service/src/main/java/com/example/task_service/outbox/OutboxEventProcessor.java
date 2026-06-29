package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import com.example.task_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Service
public class OutboxEventProcessor {

    private static final int MIN_BATCH_SIZE = 1;
    private static final int MIN_MAX_RETRIES = 1;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxPublisherProperties properties;
    private final TransactionOperations transactionOperations;

    @Autowired
    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                                OutboxEventPublisher outboxEventPublisher,
                                OutboxPublisherProperties properties,
                                ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {
        this(
            outboxEventRepository,
            outboxEventPublisher,
            properties,
            transactionOperations(transactionManagerProvider)
        );
    }

    OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                         OutboxEventPublisher outboxEventPublisher,
                         OutboxPublisherProperties properties,
                         TransactionOperations transactionOperations) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.properties = properties;
        this.transactionOperations = transactionOperations;
    }

    public int processBatch() {
        List<OutboxEventEntity> events = claimEvents();
        events.forEach(this::processClaimedEvent);
        return events.size();
    }

    private List<OutboxEventEntity> claimEvents() {
        return transactionOperations.execute(status -> {
            Pageable pageable = PageRequest.of(0, batchSize());
            List<OutboxEventEntity> events = outboxEventRepository.findClaimableEvents(
                List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                maxRetries(),
                pageable
            );
            events.forEach(OutboxEventEntity::markProcessing);
            outboxEventRepository.saveAllAndFlush(events);
            return events;
        });
    }

    private void processClaimedEvent(OutboxEventEntity event) {
        try {
            outboxEventPublisher.publish(event);
            markProcessed(event);
        } catch (RuntimeException exception) {
            markFailed(event, exception);
            log.warn(
                "Outbox event publish failed: eventId={}, eventType={}, aggregateType={}, aggregateId={}, error={}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                errorMessage(exception)
            );
        }
    }

    private void markProcessed(OutboxEventEntity event) {
        transactionOperations.executeWithoutResult(status -> {
            event.markProcessed();
            outboxEventRepository.saveAndFlush(event);
        });
    }

    private void markFailed(OutboxEventEntity event, RuntimeException exception) {
        transactionOperations.executeWithoutResult(status -> {
            event.markFailed(errorMessage(exception));
            outboxEventRepository.saveAndFlush(event);
        });
    }

    private int batchSize() {
        return Math.max(MIN_BATCH_SIZE, properties.getBatchSize());
    }

    private int maxRetries() {
        return Math.max(MIN_MAX_RETRIES, properties.getMaxRetries());
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        String normalized = message.replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

    private static TransactionOperations transactionOperations(
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider
    ) {
        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();
        if (transactionManager == null) {
            return new TransactionOperations() {
                @Override
                public <T> T execute(TransactionCallback<T> action) {
                    throw new IllegalStateException("Outbox publisher requires a transaction manager");
                }
            };
        }
        return new TransactionTemplate(transactionManager);
    }
}

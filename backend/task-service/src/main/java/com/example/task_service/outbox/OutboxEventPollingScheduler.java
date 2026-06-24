package com.example.task_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically polls task-service outbox events and delegates processing to the outbox processor.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxEventPollingScheduler {

    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-millis:5000}")
    public void poll() {
        outboxEventProcessor.processBatch();
    }
}

package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

    @Override
    public void publish(OutboxEventEntity event) {
        log.info(
            "Outbox event ready for delivery: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
            event.getEventId(),
            event.getEventType(),
            event.getAggregateType(),
            event.getAggregateId()
        );
    }
}

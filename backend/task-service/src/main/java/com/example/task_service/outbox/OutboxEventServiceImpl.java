package com.example.task_service.outbox;

import com.example.task_service.entity.OutboxEventEntity;
import com.example.task_service.enumeration.OutboxEventStatus;
import com.example.task_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public void saveNewEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
        validate(aggregateType, aggregateId, eventType, payload);

        outboxEventRepository.save(new OutboxEventEntity(
            aggregateType.trim(),
            aggregateId,
            eventType.trim(),
            payload,
            OutboxEventStatus.NEW
        ));
    }

    private void validate(String aggregateType, UUID aggregateId, String eventType, String payload) {
        if (isBlank(aggregateType)) {
            throw new IllegalArgumentException("Aggregate type is required");
        }
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID is required");
        }
        if (isBlank(eventType)) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (isBlank(payload)) {
            throw new IllegalArgumentException("Payload is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

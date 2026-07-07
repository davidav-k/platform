package com.example.ai_service.outbox;

import java.util.UUID;

public interface OutboxEventService {

    void saveNewEvent(String aggregateType, UUID aggregateId, String eventType, String payload);
}

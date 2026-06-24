package com.example.notification_service.kafka;

import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.kafka.dto.TaskAssignedEventPayload;
import com.example.notification_service.kafka.dto.TaskCreatedEventPayload;
import com.example.notification_service.kafka.dto.TaskStatusChangedEventPayload;
import com.example.notification_service.usecase.CreateSystemNotificationUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventNotificationProcessor {

    private static final String TASK_CREATED = "TASK_CREATED";
    private static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    private static final String SOURCE_SERVICE = "task-service";
    private static final String SOURCE_ENTITY_TYPE = "TASK";

    private final CreateSystemNotificationUseCase createSystemNotificationUseCase;
    private final ObjectMapper objectMapper;

    public void process(KafkaOutboxEventMessage event) {
        switch (event.eventType()) {
            case TASK_CREATED -> processTaskCreated(event);
            case TASK_ASSIGNED -> processTaskAssigned(event);
            case TASK_STATUS_CHANGED -> processTaskStatusChanged(event);
            default -> log.warn("Ignoring unsupported notification event type: eventId={}, eventType={}",
                event.eventId(), event.eventType());
        }
    }

    private void processTaskCreated(KafkaOutboxEventMessage event) {
        TaskCreatedEventPayload payload = deserialize(event.payload(), TaskCreatedEventPayload.class);
        if (payload.assigneeUserId() == null) {
            log.debug("Skipping TASK_CREATED notification without assignee: eventId={}, taskId={}",
                event.eventId(), payload.taskId());
            return;
        }

        createSystemNotificationUseCase.create(new CreateSystemNotificationRequest(
            payload.assigneeUserId(),
            NotificationType.TASK_CREATED,
            "Task created",
            "Task \"" + payload.title() + "\" was created",
            SOURCE_SERVICE,
            SOURCE_ENTITY_TYPE,
            payload.taskId()
        ));
    }

    private void processTaskAssigned(KafkaOutboxEventMessage event) {
        TaskAssignedEventPayload payload = deserialize(event.payload(), TaskAssignedEventPayload.class);
        if (payload.newAssigneeUserId() == null) {
            log.debug("Skipping TASK_ASSIGNED notification without new assignee: eventId={}, taskId={}",
                event.eventId(), payload.taskId());
            return;
        }

        createSystemNotificationUseCase.create(new CreateSystemNotificationRequest(
            payload.newAssigneeUserId(),
            NotificationType.TASK_ASSIGNED,
            "Task assigned",
            "Task \"" + payload.title() + "\" was assigned to you",
            SOURCE_SERVICE,
            SOURCE_ENTITY_TYPE,
            payload.taskId()
        ));
    }

    private void processTaskStatusChanged(KafkaOutboxEventMessage event) {
        TaskStatusChangedEventPayload payload = deserialize(event.payload(), TaskStatusChangedEventPayload.class);
        if (payload.assigneeUserId() == null) {
            log.debug("Skipping TASK_STATUS_CHANGED notification without assignee: eventId={}, taskId={}",
                event.eventId(), payload.taskId());
            return;
        }

        createSystemNotificationUseCase.create(new CreateSystemNotificationRequest(
            payload.assigneeUserId(),
            NotificationType.SYSTEM,
            "Task status changed",
            "Task \"" + payload.title() + "\" status changed from "
                + payload.previousStatus() + " to " + payload.newStatus(),
            SOURCE_SERVICE,
            SOURCE_ENTITY_TYPE,
            payload.taskId()
        ));
    }

    private <T> T deserialize(String payload, Class<T> payloadType) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka task event payload is not valid", exception);
        }
    }
}

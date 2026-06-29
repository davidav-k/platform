package com.example.notification_service.kafka;

import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.ConsumedEventEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.repository.ConsumedEventRepository;
import com.example.notification_service.usecase.CreateSystemNotificationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ASSIGNEE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PREVIOUS_ASSIGNEE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID CREATED_BY_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");

    private FakeConsumedEventRepository consumedEventRepository;
    private FakeCreateSystemNotificationUseCase createSystemNotificationUseCase;
    private ObjectMapper objectMapper;
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumedEventRepository = new FakeConsumedEventRepository();
        createSystemNotificationUseCase = new FakeCreateSystemNotificationUseCase();
        objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        TaskEventNotificationProcessor processor = new TaskEventNotificationProcessor(
            createSystemNotificationUseCase,
            objectMapper
        );
        consumer = new NotificationEventConsumer(consumedEventRepository.proxy(), objectMapper, processor);
    }

    @Test
    void taskAssignedCreatesOneNotificationAndRecordsConsumption() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(taskEvent("TASK_ASSIGNED", taskAssignedPayload())));

        assertThat(createSystemNotificationUseCase.requests()).hasSize(1);
        CreateSystemNotificationRequest request = createSystemNotificationUseCase.requests().get(0);
        assertThat(request.recipientUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(request.type()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(request.title()).isEqualTo("Task assigned");
        assertThat(request.message()).isEqualTo("Task \"Write tests\" was assigned to you");
        assertThat(request.sourceService()).isEqualTo("task-service");
        assertThat(request.sourceEntityType()).isEqualTo("TASK");
        assertThat(request.sourceEntityId()).isEqualTo(TASK_ID);
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
    }

    @Test
    void duplicateTaskAssignedEventDoesNotCreateSecondNotification() throws Exception {
        String message = objectMapper.writeValueAsString(taskEvent("TASK_ASSIGNED", taskAssignedPayload()));

        consumer.consume(message);
        consumer.consume(message);

        assertThat(consumedEventRepository.existsChecks()).isEqualTo(2);
        assertThat(createSystemNotificationUseCase.requests()).hasSize(1);
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
    }

    @Test
    void taskCreatedWithoutAssigneeDoesNotCreateNotificationButRecordsConsumption() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(taskEvent("TASK_CREATED", taskCreatedPayload(null))));

        assertThat(createSystemNotificationUseCase.requests()).isEmpty();
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
        ConsumedEventEntity saved = consumedEventRepository.savedEvents().get(0);
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getEventType()).isEqualTo("TASK_CREATED");
    }

    @Test
    void taskCreatedWithAssigneeCreatesTaskCreatedNotification() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(taskEvent("TASK_CREATED", taskCreatedPayload(ASSIGNEE_ID))));

        assertThat(createSystemNotificationUseCase.requests()).hasSize(1);
        CreateSystemNotificationRequest request = createSystemNotificationUseCase.requests().get(0);
        assertThat(request.recipientUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(request.type()).isEqualTo(NotificationType.TASK_CREATED);
        assertThat(request.title()).isEqualTo("Task created");
        assertThat(request.message()).isEqualTo("Task \"Write tests\" was created");
        assertThat(request.sourceService()).isEqualTo("task-service");
        assertThat(request.sourceEntityType()).isEqualTo("TASK");
        assertThat(request.sourceEntityId()).isEqualTo(TASK_ID);
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
    }

    @Test
    void taskStatusChangedCreatesNotificationForAssignee() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(taskEvent(
            "TASK_STATUS_CHANGED",
            taskStatusChangedPayload()
        )));

        assertThat(createSystemNotificationUseCase.requests()).hasSize(1);
        CreateSystemNotificationRequest request = createSystemNotificationUseCase.requests().get(0);
        assertThat(request.recipientUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(request.type()).isEqualTo(NotificationType.SYSTEM);
        assertThat(request.title()).isEqualTo("Task status changed");
        assertThat(request.message()).isEqualTo("Task \"Write tests\" status changed from TODO to DONE");
        assertThat(request.sourceEntityId()).isEqualTo(TASK_ID);
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
    }

    @Test
    void unsupportedEventTypeIsHandledSafely() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(taskEvent("TASK_DELETED", "{\"taskId\":\"" + TASK_ID + "\"}")));

        assertThat(createSystemNotificationUseCase.requests()).isEmpty();
        assertThat(consumedEventRepository.savedEvents()).hasSize(1);
    }

    @Test
    void failingNotificationCreationDoesNotRecordConsumption() throws Exception {
        createSystemNotificationUseCase.throwOnCreate(true);

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(
            taskEvent("TASK_ASSIGNED", taskAssignedPayload())
        ))).isInstanceOf(IllegalStateException.class);

        assertThat(createSystemNotificationUseCase.requests()).hasSize(1);
        assertThat(consumedEventRepository.savedEvents()).isEmpty();
    }

    private KafkaOutboxEventMessage taskEvent(String eventType, String payload) {
        return new KafkaOutboxEventMessage(
            EVENT_ID,
            eventType,
            "TASK",
            TASK_ID,
            OffsetDateTime.parse("2026-06-23T10:15:30Z"),
            1,
            payload
        );
    }

    private String taskCreatedPayload(UUID assigneeUserId) {
        return """
            {
              "taskId": "%s",
              "title": "Write tests",
              "description": "Add focused unit tests",
              "status": "TODO",
              "priority": "HIGH",
              "assigneeUserId": %s,
              "createdByUserId": "%s",
              "createdAt": "2026-06-23T10:15:30Z"
            }
            """.formatted(
                TASK_ID,
                assigneeUserId == null ? "null" : "\"" + assigneeUserId + "\"",
                CREATED_BY_ID
            );
    }

    private String taskAssignedPayload() {
        return """
            {
              "taskId": "%s",
              "title": "Write tests",
              "status": "TODO",
              "priority": "HIGH",
              "previousAssigneeUserId": "%s",
              "newAssigneeUserId": "%s",
              "createdByUserId": "%s",
              "updatedAt": "2026-06-23T10:15:30Z"
            }
            """.formatted(TASK_ID, PREVIOUS_ASSIGNEE_ID, ASSIGNEE_ID, CREATED_BY_ID);
    }

    private String taskStatusChangedPayload() {
        return """
            {
              "taskId": "%s",
              "title": "Write tests",
              "previousStatus": "TODO",
              "newStatus": "DONE",
              "priority": "HIGH",
              "assigneeUserId": "%s",
              "createdByUserId": "%s",
              "updatedAt": "2026-06-23T10:15:30Z"
            }
            """.formatted(TASK_ID, ASSIGNEE_ID, CREATED_BY_ID);
    }

    private static class FakeCreateSystemNotificationUseCase implements CreateSystemNotificationUseCase {

        private final List<CreateSystemNotificationRequest> requests = new ArrayList<>();
        private boolean throwOnCreate;

        @Override
        public NotificationResponse create(CreateSystemNotificationRequest request) {
            requests.add(request);
            if (throwOnCreate) {
                throw new IllegalStateException("notification creation failed");
            }
            return new NotificationResponse(
                UUID.randomUUID(),
                request.recipientUserId(),
                request.type(),
                NotificationChannel.IN_APP,
                request.title(),
                request.message(),
                NotificationStatus.PENDING,
                OffsetDateTime.parse("2026-06-23T10:15:30Z"),
                OffsetDateTime.parse("2026-06-23T10:15:30Z"),
                null,
                null
            );
        }

        List<CreateSystemNotificationRequest> requests() {
            return requests;
        }

        void throwOnCreate(boolean throwOnCreate) {
            this.throwOnCreate = throwOnCreate;
        }
    }

    private static class FakeConsumedEventRepository implements InvocationHandler {

        private final Set<UUID> existingEventIds = new HashSet<>();
        private final List<ConsumedEventEntity> savedEvents = new ArrayList<>();
        private int existsChecks;

        ConsumedEventRepository proxy() {
            return (ConsumedEventRepository) Proxy.newProxyInstance(
                ConsumedEventRepository.class.getClassLoader(),
                new Class<?>[]{ConsumedEventRepository.class},
                this
            );
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByEventId" -> existsByEventId((UUID) args[0]);
                case "save", "saveAndFlush" -> save((ConsumedEventEntity) args[0]);
                case "toString" -> "FakeConsumedEventRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private boolean existsByEventId(UUID eventId) {
            existsChecks++;
            return existingEventIds.contains(eventId);
        }

        private ConsumedEventEntity save(ConsumedEventEntity event) {
            savedEvents.add(event);
            existingEventIds.add(event.getEventId());
            return event;
        }

        List<ConsumedEventEntity> savedEvents() {
            return savedEvents;
        }

        int existsChecks() {
            return existsChecks;
        }
    }
}

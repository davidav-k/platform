package com.example.notification_service.kafka;

import com.example.notification_service.entity.ConsumedEventEntity;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.repository.ConsumedEventRepository;
import com.example.notification_service.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "notification.kafka.enabled=true",
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("test")
@Testcontainers
class NotificationKafkaDuplicateDeliveryIntegrationTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID NEW_ASSIGNEE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PREVIOUS_ASSIGNEE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID CREATED_BY_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_kafka_duplicate_delivery_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private NotificationEventConsumer notificationEventConsumer;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ConsumedEventRepository consumedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        consumedEventRepository.deleteAll();
    }

    @Test
    void duplicateTaskAssignedDeliveryCreatesOneNotificationAndOneConsumptionLog() throws Exception {
        String message = objectMapper.writeValueAsString(taskAssignedEvent());

        notificationEventConsumer.consume(message);
        notificationEventConsumer.consume(message);

        List<ConsumedEventEntity> consumedEvents = consumedEventRepository.findAll();
        assertThat(consumedEvents).hasSize(1);
        assertThat(consumedEvents.get(0).getEventId()).isEqualTo(EVENT_ID);
        assertThat(consumedEvents.get(0).getEventType()).isEqualTo("TASK_ASSIGNED");
        assertThat(consumedEventRepository.findByEventId(EVENT_ID)).isPresent();

        List<NotificationEntity> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);

        NotificationEntity notification = notifications.get(0);
        assertThat(notification.getRecipientUserId()).isEqualTo(NEW_ASSIGNEE_ID);
        assertThat(notification.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(notification.getSubject()).isEqualTo("Task assigned");
        assertThat(notification.getBody()).isEqualTo("Task \"Write integration test\" was assigned to you");
        assertThat(notification.getSourceService()).isEqualTo("task-service");
        assertThat(notification.getSourceEntityType()).isEqualTo("TASK");
        assertThat(notification.getSourceEntityId()).isEqualTo(TASK_ID);
    }

    private KafkaOutboxEventMessage taskAssignedEvent() {
        return new KafkaOutboxEventMessage(
            EVENT_ID,
            "TASK_ASSIGNED",
            "TASK",
            TASK_ID,
            OffsetDateTime.parse("2026-06-23T10:15:30Z"),
            1,
            taskAssignedPayload()
        );
    }

    private String taskAssignedPayload() {
        return """
            {
              "taskId": "%s",
              "title": "Write integration test",
              "status": "TODO",
              "priority": "HIGH",
              "previousAssigneeUserId": "%s",
              "newAssigneeUserId": "%s",
              "createdByUserId": "%s",
              "updatedAt": "2026-06-23T10:15:30Z"
            }
            """.formatted(TASK_ID, PREVIOUS_ASSIGNEE_ID, NEW_ASSIGNEE_ID, CREATED_BY_ID);
    }
}

package com.example.notification_service.usecase;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.CreateSystemNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.entity.OutboxEventEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.enumeration.OutboxEventStatus;
import com.example.notification_service.repository.OutboxEventRepository;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.usecase.CreateSystemNotificationUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import jakarta.validation.ConstraintViolationException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@ActiveProfiles("test")
@Testcontainers
class CreateNotificationUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_create_use_case_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CreateNotificationUseCase createNotificationUseCase;

    @Autowired
    private CreateSystemNotificationUseCase createSystemNotificationUseCase;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void createsPendingNotificationAndNotificationCreatedOutboxEvent() throws Exception {
        UUID recipientUserId = UUID.randomUUID();
        CreateNotificationRequest request = new CreateNotificationRequest(
                recipientUserId,
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.EMAIL,
                "  Task assigned  ",
                "  A task was assigned to you.  "
        );

        NotificationResponse response = createNotificationUseCase.create(request);

        assertThat(response.notificationId()).isNotNull();
        assertThat(response.recipientUserId()).isEqualTo(recipientUserId);
        assertThat(response.type()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.subject()).isEqualTo("Task assigned");
        assertThat(response.body()).isEqualTo("A task was assigned to you.");
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);

        NotificationEntity persisted = notificationRepository.findByNotificationId(response.notificationId())
                .orElseThrow();
        assertThat(persisted.getRecipientUserId()).isEqualTo(recipientUserId);
        assertThat(persisted.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(persisted.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(persisted.getSubject()).isEqualTo("Task assigned");
        assertThat(persisted.getBody()).isEqualTo("A task was assigned to you.");
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.PENDING);

        OutboxEventEntity event = outboxEventRepository.findAll().get(0);
        assertThat(event.getAggregateType()).isEqualTo("NOTIFICATION");
        assertThat(event.getAggregateId()).isEqualTo(response.notificationId());
        assertThat(event.getEventType()).isEqualTo("NOTIFICATION_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.NEW);

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("notificationId").asText()).isEqualTo(response.notificationId().toString());
        assertThat(payload.get("recipientUserId").asText()).isEqualTo(recipientUserId.toString());
        assertThat(payload.get("type").asText()).isEqualTo("TASK_ASSIGNED");
        assertThat(payload.get("channel").asText()).isEqualTo("EMAIL");
        assertThat(payload.get("status").asText()).isEqualTo("PENDING");
        assertThat(payload.get("createdAt").isTextual()).isTrue();
        assertThat(payload.get("updatedAt").isTextual()).isTrue();
    }

    @Test
    void createsSystemNotificationAndNotificationSystemCreatedOutboxEvent() throws Exception {
        UUID recipientUserId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        NotificationResponse response = createSystemNotificationUseCase.create(
                new CreateSystemNotificationRequest(
                        recipientUserId,
                        NotificationType.TASK_ASSIGNED,
                        "Task assigned",
                        "A task was assigned to you.",
                        "task-service",
                        "TASK",
                        taskId
                )
        );

        OutboxEventEntity event = outboxEventRepository.findAll().get(0);
        assertThat(event.getAggregateId()).isEqualTo(response.notificationId());
        assertThat(event.getEventType()).isEqualTo("NOTIFICATION_SYSTEM_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.NEW);

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("notificationId").asText()).isEqualTo(response.notificationId().toString());
        assertThat(payload.get("recipientUserId").asText()).isEqualTo(recipientUserId.toString());
        assertThat(payload.get("sourceService").asText()).isEqualTo("task-service");
        assertThat(payload.get("sourceEntityType").asText()).isEqualTo("TASK");
        assertThat(payload.get("sourceEntityId").asText()).isEqualTo(taskId.toString());
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> createNotificationUseCase.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Create notification request is required");
    }

    @Test
    void rejectsMissingRecipientUserId() {
        CreateNotificationRequest request = validRequest().withRecipientUserId(null);


        assertThatThrownBy(() -> createNotificationUseCase.create(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsBlankBody() {
        CreateNotificationRequest request = validRequest().withBody("  ");

        assertThatThrownBy(() -> createNotificationUseCase.create(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsBodyLongerThanFiveThousandCharacters() {
        CreateNotificationRequest request = validRequest().withBody("a".repeat(5001));

        assertThatThrownBy(() -> createNotificationUseCase.create(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private CreateNotificationRequest validRequest() {
        return new CreateNotificationRequest(
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification."
        );
    }
}

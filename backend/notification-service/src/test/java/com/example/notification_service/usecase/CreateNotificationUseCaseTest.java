package com.example.notification_service.usecase;

import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.repository.NotificationRepository;
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
    private NotificationRepository notificationRepository;

    @Test
    void createsPendingNotificationAndPersistsTrimmedValues() {
        UUID recipientUserId = UUID.randomUUID();
        CreateNotificationRequest request = new CreateNotificationRequest(
                recipientUserId,
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.EMAIL,
                "  Task assigned  ",
                "  A task was assigned to you.  "
        );

        NotificationResponse response = createNotificationUseCase.create(request);

        assertThat(response.getNotificationId()).isNotNull();
        assertThat(response.getRecipientUserId()).isEqualTo(recipientUserId);
        assertThat(response.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.getSubject()).isEqualTo("Task assigned");
        assertThat(response.getBody()).isEqualTo("A task was assigned to you.");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.PENDING);

        NotificationEntity persisted = notificationRepository.findByNotificationId(response.getNotificationId())
                .orElseThrow();
        assertThat(persisted.getRecipientUserId()).isEqualTo(recipientUserId);
        assertThat(persisted.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(persisted.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(persisted.getSubject()).isEqualTo("Task assigned");
        assertThat(persisted.getBody()).isEqualTo("A task was assigned to you.");
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> createNotificationUseCase.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Create notification request is required");
    }

    @Test
    void rejectsMissingRecipientUserId() {
        CreateNotificationRequest request = validRequest();
        request.setRecipientUserId(null);

        assertThatThrownBy(() -> createNotificationUseCase.create(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsBlankBody() {
        CreateNotificationRequest request = validRequest();
        request.setBody("   ");

        assertThatThrownBy(() -> createNotificationUseCase.create(request))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsBodyLongerThanFiveThousandCharacters() {
        CreateNotificationRequest request = validRequest();
        request.setBody("a".repeat(5001));

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

package com.example.notification_service.usecase;

import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import com.example.notification_service.exception.NotificationNotFoundException;
import com.example.notification_service.repository.NotificationRepository;
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
class GetNotificationUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_get_use_case_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GetNotificationUseCase getNotificationUseCase;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void returnsNotificationByPublicNotificationId() {
        UUID notificationId = UUID.randomUUID();
        NotificationEntity entity = notificationRepository.saveAndFlush(notification(notificationId));

        NotificationResponse response = getNotificationUseCase.getByNotificationId(notificationId);

        assertThat(response.notificationId()).isEqualTo(notificationId);
        assertThat(response.recipientUserId()).isEqualTo(entity.getRecipientUserId());
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void missingNotificationThrowsNotFound() {
        UUID notificationId = UUID.randomUUID();

        assertThatThrownBy(() -> getNotificationUseCase.getByNotificationId(notificationId))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining(notificationId.toString());
    }

    @Test
    void nullIdThrowsIllegalArgument() {
        assertThatThrownBy(() -> getNotificationUseCase.getByNotificationId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification ID is required");
    }

    private NotificationEntity notification(UUID notificationId) {
        return new NotificationEntity(
                notificationId,
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification.",
                NotificationStatus.PENDING
        );
    }
}

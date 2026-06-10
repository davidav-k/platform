package com.example.notification_service.repository;

import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
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

@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_repository_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsNotificationWithUuidEnumsTimestampsAndVersion() {
        UUID notificationId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        NotificationEntity saved = notificationRepository.saveAndFlush(new NotificationEntity(
                notificationId,
                recipientUserId,
                NotificationType.TASK_ASSIGNED,
                NotificationChannel.EMAIL,
                "Task assigned",
                "A task was assigned to you.",
                NotificationStatus.PENDING
        ));

        entityManager.clear();

        NotificationEntity found = notificationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getNotificationId()).isEqualTo(notificationId);
        assertThat(found.getRecipientUserId()).isEqualTo(recipientUserId);
        assertThat(found.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(found.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getVersion()).isNotNull();
    }

    @Test
    void rejectsDuplicateNotificationId() {
        UUID notificationId = UUID.randomUUID();

        notificationRepository.saveAndFlush(new NotificationEntity(
                notificationId,
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification.",
                NotificationStatus.PENDING
        ));

        NotificationEntity duplicate = new NotificationEntity(
                notificationId,
                UUID.randomUUID(),
                NotificationType.TASK_CREATED,
                NotificationChannel.IN_APP,
                "Task created",
                "A task was created.",
                NotificationStatus.PENDING
        );

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsNotificationByNotificationId() {
        UUID notificationId = UUID.randomUUID();

        notificationRepository.saveAndFlush(new NotificationEntity(
                notificationId,
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                NotificationChannel.IN_APP,
                "System notice",
                "A system notification.",
                NotificationStatus.PENDING
        ));

        assertThat(notificationRepository.findByNotificationId(notificationId))
                .isPresent()
                .get()
                .extracting(NotificationEntity::getNotificationId)
                .isEqualTo(notificationId);
    }
}

package com.example.notification_service.repository;

import com.example.notification_service.entity.NotificationPreferenceEntity;
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
class NotificationPreferenceRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_preference_repository_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsPreferenceByUserId() {
        UUID userId = UUID.randomUUID();

        NotificationPreferenceEntity saved = notificationPreferenceRepository.saveAndFlush(
                new NotificationPreferenceEntity(userId, false, true)
        );

        entityManager.clear();

        NotificationPreferenceEntity found = notificationPreferenceRepository.findByUserId(userId).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.isEmailEnabled()).isFalse();
        assertThat(found.isInAppEnabled()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getVersion()).isNotNull();
        assertThat(notificationPreferenceRepository.existsByUserId(userId)).isTrue();
    }

    @Test
    void rejectsDuplicateUserId() {
        UUID userId = UUID.randomUUID();

        notificationPreferenceRepository.saveAndFlush(new NotificationPreferenceEntity(userId));

        NotificationPreferenceEntity duplicate = new NotificationPreferenceEntity(userId, false, false);

        assertThatThrownBy(() -> notificationPreferenceRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usesJavaDefaultsForEnabledChannels() {
        UUID userId = UUID.randomUUID();

        NotificationPreferenceEntity saved = notificationPreferenceRepository.saveAndFlush(
                new NotificationPreferenceEntity(userId)
        );

        entityManager.clear();

        NotificationPreferenceEntity found = notificationPreferenceRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isEmailEnabled()).isTrue();
        assertThat(found.isInAppEnabled()).isTrue();
    }
}

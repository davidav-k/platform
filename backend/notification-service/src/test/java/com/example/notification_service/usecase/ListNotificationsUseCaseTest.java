package com.example.notification_service.usecase;

import com.example.notification_service.dto.NotificationListQuery;
import com.example.notification_service.dto.NotificationListResponse;
import com.example.notification_service.dto.NotificationResponse;
import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.enumeration.NotificationChannel;
import com.example.notification_service.enumeration.NotificationStatus;
import com.example.notification_service.enumeration.NotificationType;
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

import java.util.List;
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
class ListNotificationsUseCaseTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"))
            .withDatabaseName("notification_list_use_case_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ListNotificationsUseCase listNotificationsUseCase;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID firstRecipient;
    private UUID secondRecipient;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        firstRecipient = UUID.randomUUID();
        secondRecipient = UUID.randomUUID();
        notificationRepository.saveAllAndFlush(List.of(
                notification(firstRecipient, NotificationStatus.PENDING,
                        NotificationChannel.EMAIL, NotificationType.TASK_ASSIGNED, "First"),
                notification(secondRecipient, NotificationStatus.SENT,
                        NotificationChannel.IN_APP, NotificationType.SYSTEM, "Second"),
                notification(firstRecipient, NotificationStatus.FAILED,
                        NotificationChannel.EMAIL, NotificationType.TASK_CREATED, "Third")
        ));
    }

    @Test
    void listsAllNotificationsWithPaginationMetadata() {
        NotificationListResponse response = list(query(null, null, null, null, 0, 2, null));

        assertThat(response.items()).hasSize(2);
        assertThat(response.page().number()).isZero();
        assertThat(response.page().size()).isEqualTo(2);
        assertThat(response.page().totalElements()).isEqualTo(3);
        assertThat(response.page().totalPages()).isEqualTo(2);
    }

    @Test
    void filtersByRecipientUserId() {
        assertThat(list(query(firstRecipient, null, null, null, 0, 20, null)).items())
                .hasSize(2)
                .allMatch(item -> firstRecipient.equals(item.recipientUserId()));
    }

    @Test
    void filtersByStatus() {
        assertSingleMatch(query(null, NotificationStatus.SENT, null, null, 0, 20, null),
                item -> item.status() == NotificationStatus.SENT);
    }

    @Test
    void filtersByChannel() {
        assertSingleMatch(query(null, null, NotificationChannel.IN_APP, null, 0, 20, null),
                item -> item.channel() == NotificationChannel.IN_APP);
    }

    @Test
    void filtersByType() {
        assertSingleMatch(query(null, null, null, NotificationType.TASK_CREATED, 0, 20, null),
                item -> item.type() == NotificationType.TASK_CREATED);
    }

    @Test
    void supportsSortingByAllowedFields() {
        for (String field : List.of("createdAt", "updatedAt", "status", "channel", "type")) {
            assertThat(list(query(null, null, null, null, 0, 20, field + ",asc")).items()).hasSize(3);
        }
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> list(query(null, null, null, null, -1, 20, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be greater than or equal to 0");
    }

    @Test
    void rejectsInvalidSize() {
        assertThatThrownBy(() -> list(query(null, null, null, null, 0, 101, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be between 1 and 100");
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertThatThrownBy(() -> list(query(null, null, null, null, 0, 20, "id,asc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported sort field: id");
    }

    @Test
    void rejectsUnsupportedSortDirection() {
        assertThatThrownBy(() -> list(query(null, null, null, null, 0, 20, "createdAt,sideways")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported sort direction: sideways");
    }

    @Test
    void responseDoesNotExposeInternalDatabaseId() throws Exception {
        NotificationListResponse response = list(query(null, null, null, null, 0, 20, null));

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).doesNotContain("\"id\"");
    }

    private NotificationListResponse list(NotificationListQuery query) {
        return listNotificationsUseCase.list(query);
    }

    private NotificationListQuery query(UUID recipientUserId, NotificationStatus status,
                                        NotificationChannel channel, NotificationType type,
                                        int page, int size, String sort) {
        return new NotificationListQuery(recipientUserId, status, channel, type, page, size, sort);
    }

    private NotificationEntity notification(UUID recipientUserId, NotificationStatus status,
                                            NotificationChannel channel, NotificationType type,
                                            String subject) {
        NotificationEntity notification = new NotificationEntity(
                null, recipientUserId, type, channel, subject, subject + " body", status
        );
        if (status == NotificationStatus.FAILED) {
            notification.setFailureReason("Delivery failed");
        }
        return notification;
    }

    private void assertSingleMatch(NotificationListQuery query,
                                   java.util.function.Predicate<NotificationResponse> predicate) {
        assertThat(list(query).items()).singleElement().matches(predicate);
    }
}

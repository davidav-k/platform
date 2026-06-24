package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

class TaskNotificationPublisherTest {

    private final NotificationClient notificationClient = mock(NotificationClient.class);
    private final NotificationClientProperties properties = new NotificationClientProperties();
    private final TaskNotificationPublisher publisher =
        new TaskNotificationPublisherImpl(notificationClient, properties);

    @Test
    void disabledIntegrationDoesNotCallClient() {
        properties.setEnabled(false);
        properties.setAssignmentRestEnabled(true);

        publisher.notifyTaskAssigned(context(UUID.randomUUID()));

        verify(notificationClient, never()).createNotification(any());
    }

    @Test
    void disabledAssignmentRestNotificationDoesNotCallClient() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(false);

        publisher.notifyTaskAssigned(context(UUID.randomUUID()));

        verify(notificationClient, never()).createNotification(any());
    }

    @Test
    void nullContextDoesNotCallClient() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(true);

        publisher.notifyTaskAssigned(null);

        verify(notificationClient, never()).createNotification(any());
    }

    @Test
    void nullAssigneeDoesNotCallClient() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(true);
        TaskNotificationContext context = new TaskNotificationContext(
            UUID.randomUUID(), "Task title", null, UUID.randomUUID()
        );

        publisher.notifyTaskAssigned(context);

        verify(notificationClient, never()).createNotification(any());
    }

    @Test
    void validAssigneeCallsClientWithExpectedRequest() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(true);
        UUID assigneeUserId = UUID.randomUUID();
        ArgumentCaptor<CreateNotificationRequest> captor =
            ArgumentCaptor.forClass(CreateNotificationRequest.class);

        publisher.notifyTaskAssigned(context(assigneeUserId));

        verify(notificationClient).createNotification(captor.capture());
        CreateNotificationRequest request = captor.getValue();
        assertThat(request.recipientUserId()).isEqualTo(assigneeUserId);
        assertThat(request.type()).isEqualTo("TASK_ASSIGNED");
        assertThat(request.title()).isEqualTo("New task assigned");
        assertThat(request.message()).isEqualTo("Task \"Task title\" was assigned to you");
        assertThat(request.sourceService()).isEqualTo("task-service");
        assertThat(request.sourceEntityType()).isEqualTo("TASK");
        assertThat(request.sourceEntityId()).isNotNull();
    }

    @Test
    void selfAssignedTaskDoesNotCallClient() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(true);
        UUID userId = UUID.randomUUID();

        publisher.notifyTaskAssigned(new TaskNotificationContext(
            UUID.randomUUID(), "Task title", userId, userId
        ));

        verify(notificationClient, never()).createNotification(any());
    }

    @Test
    void clientExceptionDoesNotPropagate() {
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(true);
        org.mockito.Mockito.doThrow(new NotificationClientException("request failed", null))
            .when(notificationClient).createNotification(any());

        assertThatCode(() -> publisher.notifyTaskAssigned(context(UUID.randomUUID())))
            .doesNotThrowAnyException();
    }

    private TaskNotificationContext context(UUID assigneeUserId) {
        return new TaskNotificationContext(
            UUID.randomUUID(), "Task title", assigneeUserId, UUID.randomUUID()
        );
    }
}

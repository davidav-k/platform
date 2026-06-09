package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Publishes best-effort task assignment notifications. */
@Component
public class TaskNotificationPublisherImpl implements TaskNotificationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskNotificationPublisherImpl.class);

    private final NotificationClient notificationClient;
    private final NotificationClientProperties properties;

    public TaskNotificationPublisherImpl(NotificationClient notificationClient,
                                         NotificationClientProperties properties) {
        this.notificationClient = notificationClient;
        this.properties = properties;
    }

    @Override
    public void notifyTaskAssigned(TaskNotificationContext context) {
        if (!properties.isEnabled() || context == null || context.assigneeUserId() == null) {
            return;
        }

        CreateNotificationRequest request = new CreateNotificationRequest(
            context.assigneeUserId(),
            "TASK_ASSIGNED",
            "IN_APP",
            "New task assigned",
            "You have been assigned task: " + context.title()
        );

        try {
            notificationClient.createNotification(request);
        } catch (RuntimeException exception) {
            LOGGER.warn("Notification request failed for recipientUserId={} and type={}",
                request.getRecipientUserId(), request.getType(), exception);
        }
    }
}

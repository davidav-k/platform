package com.example.task_service.notification;

import com.example.task_service.notification.dto.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskNotificationPublisherImpl implements TaskNotificationPublisher {

    private final NotificationClient notificationClient;
    private final NotificationClientProperties properties;

    @Override
    public void notifyTaskAssigned(TaskNotificationContext context) {
        if (!properties.isEnabled()
            || context == null
            || context.assigneeUserId() == null
            || context.assigneeUserId().equals(context.createdByUserId())) {
            return;
        }
        if (!properties.isAssignmentRestEnabled()) {
            log.info("Skipping synchronous REST assignment notification because assignment REST notifications are disabled");
            return;
        }

        CreateNotificationRequest request = new CreateNotificationRequest(
            context.assigneeUserId(),
            "TASK_ASSIGNED",
            "New task assigned",
            "Task \"" + context.title() + "\" was assigned to you",
            "task-service",
            "TASK",
            context.taskId()
        );

        try {
            notificationClient.createNotification(request);
        } catch (RuntimeException exception) {
            log.warn("Notification request failed for recipientUserId={} and type={}",
                request.recipientUserId(), request.type(), exception);
        }
    }
}

package com.example.task_service.notification;

public interface TaskNotificationPublisher {

    void notifyTaskAssigned(TaskNotificationContext context);
}

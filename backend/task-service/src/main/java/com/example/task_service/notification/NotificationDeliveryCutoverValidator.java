package com.example.task_service.notification;

import com.example.task_service.outbox.OutboxPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs non-fatal warnings for notification cutover configurations that can
 * disable the legacy REST path before the Kafka consumer is enabled.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationDeliveryCutoverValidator implements ApplicationRunner {

    private static final String KAFKA_ADAPTER = "kafka";

    private final OutboxPublisherProperties outboxPublisherProperties;
    private final NotificationClientProperties notificationClientProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (isKafkaPublisherEnabled()
            && !notificationClientProperties.isAssignmentRestEnabled()
            && !isNotificationKafkaEnabled()) {
            log.warn("Kafka outbox publishing is enabled and synchronous REST assignment notifications are disabled, "
                + "but notification.kafka.enabled is false. Task assignment notifications may not be delivered until "
                + "the notification-service Kafka consumer is enabled.");
        }
    }

    private boolean isKafkaPublisherEnabled() {
        return outboxPublisherProperties.isEnabled()
            && KAFKA_ADAPTER.equalsIgnoreCase(outboxPublisherProperties.getAdapter());
    }

    private boolean isNotificationKafkaEnabled() {
        return environment.getProperty("notification.kafka.enabled", Boolean.class,
            environment.getProperty("NOTIFICATION_KAFKA_ENABLED", Boolean.class, false));
    }
}

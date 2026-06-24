package com.example.task_service.notification;

import com.example.task_service.outbox.OutboxPublisherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificationDeliveryCutoverValidatorTest {

    private static final ApplicationArguments NO_ARGS = new DefaultApplicationArguments();

    @Test
    void warnsWhenKafkaPublisherEnabledRestDisabledAndNotificationConsumerDisabled(CapturedOutput output) {
        OutboxPublisherProperties outboxProperties = kafkaOutboxProperties();
        NotificationClientProperties notificationProperties = notificationProperties(false);
        Environment environment = new MockEnvironment()
            .withProperty("notification.kafka.enabled", "false");

        new NotificationDeliveryCutoverValidator(outboxProperties, notificationProperties, environment).run(NO_ARGS);

        assertThat(output)
            .contains("Kafka outbox publishing is enabled")
            .contains("notification.kafka.enabled is false");
    }

    @Test
    void doesNotWarnWhenNotificationConsumerEnabled(CapturedOutput output) {
        OutboxPublisherProperties outboxProperties = kafkaOutboxProperties();
        NotificationClientProperties notificationProperties = notificationProperties(false);
        Environment environment = new MockEnvironment()
            .withProperty("notification.kafka.enabled", "true");

        new NotificationDeliveryCutoverValidator(outboxProperties, notificationProperties, environment).run(NO_ARGS);

        assertThat(output)
            .contains("Notification delivery startup configuration")
            .contains("outboxPublisherEnabled=true")
            .contains("outboxPublisherAdapter=kafka")
            .contains("assignmentRestNotificationEnabled=false")
            .doesNotContain("Task assignment notifications may not be delivered");
    }

    @Test
    void doesNotWarnWhenRestAssignmentNotificationsRemainEnabled(CapturedOutput output) {
        OutboxPublisherProperties outboxProperties = kafkaOutboxProperties();
        NotificationClientProperties notificationProperties = notificationProperties(true);

        new NotificationDeliveryCutoverValidator(outboxProperties, notificationProperties, new StandardEnvironment())
            .run(NO_ARGS);

        assertThat(output).doesNotContain("Task assignment notifications may not be delivered");
    }

    private static OutboxPublisherProperties kafkaOutboxProperties() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(true);
        properties.setAdapter("kafka");
        return properties;
    }

    private static NotificationClientProperties notificationProperties(boolean assignmentRestEnabled) {
        NotificationClientProperties properties = new NotificationClientProperties();
        properties.setEnabled(true);
        properties.setAssignmentRestEnabled(assignmentRestEnabled);
        return properties;
    }
}

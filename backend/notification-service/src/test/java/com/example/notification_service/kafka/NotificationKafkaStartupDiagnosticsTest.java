package com.example.notification_service.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificationKafkaStartupDiagnosticsTest {

    @Test
    void logsKafkaConsumerStatusAndTopic(CapturedOutput output) throws Exception {
        NotificationKafkaProperties properties = new NotificationKafkaProperties();
        properties.setEnabled(true);
        properties.setTopic("platform.task-events");

        new NotificationKafkaStartupDiagnostics(properties).run(new DefaultApplicationArguments());

        assertThat(output)
            .contains("Notification Kafka startup configuration")
            .contains("kafkaConsumerEnabled=true")
            .contains("topic=platform.task-events");
    }
}

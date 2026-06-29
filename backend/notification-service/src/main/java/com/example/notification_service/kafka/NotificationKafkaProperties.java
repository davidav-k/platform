package com.example.notification_service.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.kafka")
public class NotificationKafkaProperties {

    private boolean enabled;
    private String topic = "platform.task-events";
}

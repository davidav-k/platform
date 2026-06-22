package com.example.task_service.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuration for task-service calls to notification-service. */
@Setter
@Getter
@ConfigurationProperties(prefix = "notification-service")
public class NotificationClientProperties {

    private String baseUrl = "http://notification-service:8087";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(2);
    private boolean enabled;

}

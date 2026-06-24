package com.example.task_service.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the task-service outbox publisher.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.publisher")
public class OutboxPublisherProperties {

    private boolean enabled = true;

    private String adapter = "kafka";

    private int batchSize = 20;

    private int maxRetries = 3;

    private long fixedDelayMillis = 5000;
}

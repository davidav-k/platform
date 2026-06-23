package com.example.task_service.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuration for the local task-service outbox polling foundation. */
@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.publisher")
public class OutboxPublisherProperties {

    private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(5);
    private static final long MIN_FIXED_DELAY_MILLIS = 1L;

    private boolean enabled;
    private String adapter = "logging";
    private int batchSize = 20;
    private int maxRetries = 3;
    private Duration fixedDelay = Duration.ofSeconds(5);

    public long fixedDelayMillis() {
        Duration delay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
        return Math.max(MIN_FIXED_DELAY_MILLIS, delay.toMillis());
    }
}

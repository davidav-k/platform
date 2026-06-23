package com.example.task_service.outbox.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.publisher.kafka")
public class KafkaOutboxPublisherProperties {

    private boolean enabled;
    private String bootstrapServers = "kafka:9092";
    private String topic = "platform.task-events";
}

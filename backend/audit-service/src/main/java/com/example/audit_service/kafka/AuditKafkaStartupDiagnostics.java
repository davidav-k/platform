package com.example.audit_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Logs non-sensitive Kafka consumer startup settings for audit operations. */
@Component
public class AuditKafkaStartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaStartupDiagnostics.class);

    private final AuditKafkaProperties properties;

    public AuditKafkaStartupDiagnostics(AuditKafkaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Audit Kafka startup configuration: kafkaConsumerEnabled={}, taskTopic={}, userTopic={}, notificationTopic={}, aiTopic={}",
                properties.isEnabled(),
                properties.getTopic(),
                properties.getUserTopic(),
                properties.getNotificationTopic(),
                properties.getAiTopic()
        );
    }
}

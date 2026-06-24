package com.example.notification_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Logs non-sensitive Kafka consumer startup settings for notification delivery operations. */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaStartupDiagnostics implements ApplicationRunner {

    private final NotificationKafkaProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        log.info(
            "Notification Kafka startup configuration: kafkaConsumerEnabled={}, topic={}",
            properties.isEnabled(),
            properties.getTopic()
        );
    }
}

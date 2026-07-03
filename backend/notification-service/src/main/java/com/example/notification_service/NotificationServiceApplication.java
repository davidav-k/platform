package com.example.notification_service;

import com.example.notification_service.kafka.NotificationKafkaProperties;
import com.example.notification_service.outbox.OutboxPublisherProperties;
import com.example.notification_service.outbox.kafka.KafkaOutboxPublisherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        NotificationKafkaProperties.class,
        OutboxPublisherProperties.class,
        KafkaOutboxPublisherProperties.class
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

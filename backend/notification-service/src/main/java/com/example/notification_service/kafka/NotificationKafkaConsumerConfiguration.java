package com.example.notification_service.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true")
public class NotificationKafkaConsumerConfiguration {
}

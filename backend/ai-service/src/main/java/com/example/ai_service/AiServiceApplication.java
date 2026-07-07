package com.example.ai_service;

import com.example.ai_service.outbox.OutboxPublisherProperties;
import com.example.ai_service.outbox.kafka.KafkaOutboxPublisherProperties;
import com.example.ai_service.provider.AiProviderMetadataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        OutboxPublisherProperties.class,
        KafkaOutboxPublisherProperties.class,
        AiProviderMetadataProperties.class
})
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}

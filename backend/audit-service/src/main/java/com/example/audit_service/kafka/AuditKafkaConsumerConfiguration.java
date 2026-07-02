package com.example.audit_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true")
public class AuditKafkaConsumerConfiguration {


    @Bean
    public ConsumerFactory<String, String> auditKafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            SslBundles sslBundles
    ) {
        Map<String, Object> consumerProperties =
                new HashMap<>(kafkaProperties.buildConsumerProperties(sslBundles));
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    public DefaultErrorHandler auditKafkaErrorHandler() {
        return new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception exception) ->
                log.error(
                        "Audit Kafka event was not processed: topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception
                ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> auditKafkaListenerContainerFactory(
            ConsumerFactory<String, String> auditKafkaConsumerFactory,
            DefaultErrorHandler auditKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditKafkaConsumerFactory);
        factory.setCommonErrorHandler(auditKafkaErrorHandler);
        return factory;
    }
}

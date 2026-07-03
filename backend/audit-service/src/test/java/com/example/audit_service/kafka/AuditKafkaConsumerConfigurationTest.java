package com.example.audit_service.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuditKafkaConsumerConfigurationTest {

    @Test
    void configuresStringConsumerFactoryAndStandardErrorHandler() {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(List.of("localhost:9092"));
        properties.getConsumer().setGroupId("audit-service");
        AuditKafkaConsumerConfiguration configuration = new AuditKafkaConsumerConfiguration();

        ConsumerFactory<String, String> consumerFactory = configuration.auditKafkaConsumerFactory(
                properties,
                mock(SslBundles.class)
        );
        DefaultErrorHandler errorHandler = configuration.auditKafkaErrorHandler();
        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory =
                configuration.auditKafkaListenerContainerFactory(consumerFactory, errorHandler);

        assertThat(consumerFactory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:9092"))
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "audit-service")
                .containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        assertThat(listenerFactory).isNotNull();
        assertThat(errorHandler).isNotNull();
    }
}

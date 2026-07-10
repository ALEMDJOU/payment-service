package com.yowyob.payment.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import reactor.kafka.sender.SenderOptions;

/**
 * Configuration Kafka réactive (producteur uniquement).
 */
@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        /**
         * @return template producteur Kafka
         */
        @Bean
        public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate() {
                Map<String, Object> props = new HashMap<>();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.JsonSerializer.class);
                return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
        }
}

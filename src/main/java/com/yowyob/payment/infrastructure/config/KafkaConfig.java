package com.yowyob.payment.infrastructure.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

/**
 * Configuration Kafka réactive (producteur et consommateur).
 */
@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Value("${spring.kafka.consumer.group-id}")
        private String groupId;

        @Value("${yowyob.kafka.topic-wallet-create}")
        private String walletCreateTopic;

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

        /**
         * @return consommateur wallet-create-topic
         */
        @Bean
        public ReactiveKafkaConsumerTemplate<String, WalletCreationMessage> walletCreateConsumer() {
                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.JsonDeserializer.class);
                props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
                props.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE,
                                WalletCreationMessage.class.getName());
                ReceiverOptions<String, WalletCreationMessage> options = ReceiverOptions
                                .<String, WalletCreationMessage>create(props)
                                .subscription(Collections.singleton(walletCreateTopic));
                return new ReactiveKafkaConsumerTemplate<>(options);
        }

        /**
         * Message Kafka de création de wallet (compat yowyob-pay).
         *
         * @param ownerId   identifiant propriétaire
         * @param ownerName nom propriétaire
         */
        public record WalletCreationMessage(java.util.UUID ownerId, String ownerName) {
        }
}

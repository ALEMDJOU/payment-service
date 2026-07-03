package com.yowyob.payment.infrastructure.adapters.outbound.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.event.TransactionEvent;
import com.yowyob.payment.domain.event.TransactionEventPublisherPort;
import com.yowyob.payment.domain.event.WalletEvent;
import com.yowyob.payment.domain.event.WalletEventPublisherPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Publication des événements Kafka transaction et wallet.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventProducer implements TransactionEventPublisherPort, WalletEventPublisherPort {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;

    @Value("${yowyob.kafka.topic-transaction-events}")
    private String transactionTopic;

    @Value("${yowyob.kafka.topic-wallet-events}")
    private String walletTopic;

    @Override
    public Mono<Void> publish(TransactionEvent event) {
        return kafkaTemplate.send(transactionTopic, event.transactionId().toString(), event).then();
    }

    @Override
    public Mono<Void> publish(WalletEvent event) {
        return kafkaTemplate.send(walletTopic, event.walletId().toString(), event).then();
    }
}

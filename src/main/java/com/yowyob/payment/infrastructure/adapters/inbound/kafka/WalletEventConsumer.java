package com.yowyob.payment.infrastructure.adapters.inbound.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;

import com.yowyob.payment.application.WalletService;
import com.yowyob.payment.infrastructure.config.KafkaConfig.WalletCreationMessage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Consomme wallet-create-topic pour créer un portefeuille si absent.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "yowyob.kafka.consumer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletEventConsumer {

    private final ReactiveKafkaConsumerTemplate<String, WalletCreationMessage> consumer;
    private final WalletService walletService;

    /**
     * Démarre l'écoute Kafka au démarrage du bean.
     */
    @PostConstruct
    public void listen() {
        consumer.receive()
                .flatMap(record -> walletService.findByOwnerId(record.value().ownerId())
                        .onErrorResume(e -> walletService.createWallet(record.value().ownerId()))
                        .doOnSuccess(w -> log.info("Wallet assuré pour owner {}", record.value().ownerId()))
                        .doOnError(e -> log.error("Erreur wallet-create consumer", e))
                        .then(Mono.fromRunnable(() -> record.receiverOffset().acknowledge())))
                .onErrorContinue((e, o) -> log.error("Kafka consumer error", e))
                .subscribe();
    }
}

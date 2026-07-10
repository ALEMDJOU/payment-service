package com.yowyob.payment.application;

import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.domain.webhook.WebhookOutboxRepositoryPort;
import com.yowyob.payment.domain.webhook.WebhookOutboxStatus;
import com.yowyob.payment.infrastructure.config.WebhookProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Worker asynchrone qui livre les webhooks consommateurs depuis l'outbox.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class WebhookOutboxProcessor {

    private final WebhookOutboxRepositoryPort outboxRepository;
    private final WebhookProperties properties;
    private final WebClient consumerWebhookWebClient;

    /**
     * Traite les entrées outbox prêtes à être livrées.
     */
    @Scheduled(fixedDelayString = "${yowyob.webhook.poll-interval-ms:5000}")
    public void processPending() {
        outboxRepository.findReadyForDelivery(properties.getBatchSize())
                .flatMap(this::deliverSafely)
                .onErrorContinue((error, entry) -> log.error("Erreur traitement outbox webhook", error))
                .subscribe();
    }

    private Mono<Void> deliverSafely(WebhookOutboxEntry entry) {
        return deliver(entry)
                .onErrorResume(error -> scheduleRetry(entry, error.getMessage()));
    }

    private Mono<Void> deliver(WebhookOutboxEntry entry) {
        return consumerWebhookWebClient.post()
                .uri(entry.callbackUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(entry.payloadJson())
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        return scheduleRetry(entry, "HTTP " + response.getStatusCode().value());
                    }
                    WebhookOutboxEntry sent = entry.withStatus(WebhookOutboxStatus.SENT);
                    return outboxRepository.update(sent)
                            .doOnSuccess(saved -> log.info("Webhook consommateur livré: id={}, tx={}",
                                    saved.id(), saved.transactionId()))
                            .then();
                });
    }

    private Mono<Void> scheduleRetry(WebhookOutboxEntry entry, String errorMessage) {
        int nextAttempt = entry.attemptCount() + 1;
        if (nextAttempt >= properties.getMaxAttempts()) {
            WebhookOutboxEntry failed = entry.withRetry(nextAttempt, entry.nextAttemptAt(), truncate(errorMessage))
                    .withStatus(WebhookOutboxStatus.FAILED);
            log.warn("Webhook consommateur abandonné après {} tentatives: tx={}, url={}",
                    nextAttempt, entry.transactionId(), entry.callbackUrl());
            return outboxRepository.update(failed).then();
        }
        long delayMs = (long) (properties.getInitialDelayMs()
                * Math.pow(properties.getMultiplier(), Math.max(0, nextAttempt - 1)));
        Instant nextAttemptAt = Instant.now().plusMillis(delayMs);
        WebhookOutboxEntry retry = entry.withRetry(nextAttempt, nextAttemptAt, truncate(errorMessage))
                .withStatus(WebhookOutboxStatus.PENDING);
        log.warn("Webhook consommateur en retry {}/{} dans {}ms: tx={}, erreur={}",
                nextAttempt, properties.getMaxAttempts(), delayMs, entry.transactionId(), errorMessage);
        return outboxRepository.update(retry).then();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}

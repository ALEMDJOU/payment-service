package com.yowyob.payment.application;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;
import com.yowyob.payment.domain.webhook.ConsumerWebhookNotifierPort;
import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.domain.webhook.WebhookOutboxRepositoryPort;
import com.yowyob.payment.domain.webhook.WebhookOutboxStatus;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ConsumerWebhookPayload;
import com.yowyob.payment.infrastructure.support.JsonSupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Enfile les notifications HTTP vers le webhook fourni par le consommateur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerWebhookNotifier implements ConsumerWebhookNotifierPort {

    private final WebhookOutboxRepositoryPort outboxRepository;

    @Override
    public Mono<Void> enqueue(Transaction transaction, ConsumerWebhookEventType eventType,
            String stripeCheckoutUrl) {
        if (transaction.callbackUrl() == null || transaction.callbackUrl().isBlank()) {
            return Mono.empty();
        }
        validateCallbackUrl(transaction.callbackUrl());
        ConsumerWebhookPayload payload = ConsumerWebhookPayload.from(transaction, eventType, stripeCheckoutUrl);
        Instant now = Instant.now();
        WebhookOutboxEntry entry = new WebhookOutboxEntry(
                UUID.randomUUID(),
                transaction.id(),
                eventType,
                transaction.callbackUrl().trim(),
                JsonSupport.writeValue(payload),
                WebhookOutboxStatus.PENDING,
                0,
                now,
                null,
                now,
                now);
        return outboxRepository.save(entry)
                .doOnSuccess(saved -> log.info("Webhook consommateur enfilé: tx={}, event={}, url={}",
                        transaction.id(), eventType, saved.callbackUrl()))
                .then();
    }

    private void validateCallbackUrl(String callbackUrl) {
        URI uri = URI.create(callbackUrl.trim());
        if (uri.getScheme() == null || (!"https".equalsIgnoreCase(uri.getScheme())
                && !"http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("callbackUrl doit être une URL HTTP(S) valide");
        }
    }
}

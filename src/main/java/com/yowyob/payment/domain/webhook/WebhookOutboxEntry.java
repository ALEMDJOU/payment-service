package com.yowyob.payment.domain.webhook;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée outbox pour livraison HTTP asynchrone vers le consommateur.
 */
public record WebhookOutboxEntry(
        UUID id,
        UUID transactionId,
        ConsumerWebhookEventType eventType,
        String callbackUrl,
        String payloadJson,
        WebhookOutboxStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param status nouveau statut
     * @return entrée mise à jour
     */
    public WebhookOutboxEntry withStatus(WebhookOutboxStatus status) {
        return new WebhookOutboxEntry(id, transactionId, eventType, callbackUrl, payloadJson, status,
                attemptCount, nextAttemptAt, lastError, createdAt, Instant.now());
    }

    /**
     * @param attemptCount  nombre de tentatives
     * @param nextAttemptAt prochaine tentative
     * @param lastError     dernière erreur
     * @return entrée mise à jour
     */
    public WebhookOutboxEntry withRetry(int attemptCount, Instant nextAttemptAt, String lastError) {
        return new WebhookOutboxEntry(id, transactionId, eventType, callbackUrl, payloadJson, status,
                attemptCount, nextAttemptAt, lastError, createdAt, Instant.now());
    }
}

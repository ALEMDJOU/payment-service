package com.yowyob.payment.domain.webhook;

/**
 * Statut d'une livraison webhook dans l'outbox.
 */
public enum WebhookOutboxStatus {
    PENDING,
    SENT,
    FAILED
}

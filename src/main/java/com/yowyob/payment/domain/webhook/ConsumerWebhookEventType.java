package com.yowyob.payment.domain.webhook;

/**
 * Type d'événement envoyé au webhook consommateur.
 */
public enum ConsumerWebhookEventType {
    TRANSACTION_PENDING,
    TRANSACTION_SUCCEEDED,
    TRANSACTION_FAILED,
    TRANSACTION_CANCELLED
}

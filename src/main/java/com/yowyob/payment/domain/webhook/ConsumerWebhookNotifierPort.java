package com.yowyob.payment.domain.webhook;

import com.yowyob.payment.domain.transaction.Transaction;

import reactor.core.publisher.Mono;

/**
 * Port de notification HTTP vers les applications consommatrices.
 */
public interface ConsumerWebhookNotifierPort {

    /**
     * Enfile une notification si la transaction possède un {@code callbackUrl}.
     *
     * @param transaction       transaction courante
     * @param eventType         type d'événement
     * @param stripeCheckoutUrl URL checkout optionnelle (événement PENDING)
     * @return void
     */
    Mono<Void> enqueue(Transaction transaction, ConsumerWebhookEventType eventType, String stripeCheckoutUrl);
}

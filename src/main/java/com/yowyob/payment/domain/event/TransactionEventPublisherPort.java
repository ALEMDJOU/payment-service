package com.yowyob.payment.domain.event;

import reactor.core.publisher.Mono;

/**
 * Port sortant de publication d'événements transaction.
 */
public interface TransactionEventPublisherPort {

    /**
     * @param event événement à publier
     * @return complétion
     */
    Mono<Void> publish(TransactionEvent event);
}

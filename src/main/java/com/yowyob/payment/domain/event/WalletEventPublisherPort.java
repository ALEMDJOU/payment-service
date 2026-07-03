package com.yowyob.payment.domain.event;

import reactor.core.publisher.Mono;

/**
 * Port sortant de publication d'événements wallet.
 */
public interface WalletEventPublisherPort {

    /**
     * @param event événement à publier
     * @return complétion
     */
    Mono<Void> publish(WalletEvent event);
}

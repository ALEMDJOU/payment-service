package com.yowyob.payment.domain.webhook;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port de persistance de l'outbox webhook consommateur.
 */
public interface WebhookOutboxRepositoryPort {

    /**
     * @param entry entrée à persister
     * @return entrée sauvegardée
     */
    Mono<WebhookOutboxEntry> save(WebhookOutboxEntry entry);

    /**
     * @param entry entrée à mettre à jour
     * @return entrée mise à jour
     */
    Mono<WebhookOutboxEntry> update(WebhookOutboxEntry entry);

    /**
     * @param limit nombre max d'entrées
     * @return entrées prêtes à être livrées
     */
    Flux<WebhookOutboxEntry> findReadyForDelivery(int limit);

    /**
     * @param id identifiant
     * @return entrée
     */
    Mono<WebhookOutboxEntry> findById(UUID id);
}

package com.yowyob.payment.domain.application;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port sortant de persistance des applications API.
 */
public interface ApplicationRepositoryPort {

    /**
     * @param application application à créer
     * @return application persistée
     */
    Mono<ClientApplication> save(ClientApplication application);

    /**
     * @return toutes les applications
     */
    Flux<ClientApplication> findAll();

    /**
     * @param id identifiant
     * @return application ou vide
     */
    Mono<ClientApplication> findById(UUID id);

    /**
     * @return toutes les applications actives
     */
    Flux<ClientApplication> findAllActive();
}

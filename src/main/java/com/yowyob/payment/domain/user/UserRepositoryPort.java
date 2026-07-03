package com.yowyob.payment.domain.user;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port sortant de persistance des utilisateurs.
 */
public interface UserRepositoryPort {

    /**
     * @param user utilisateur à persister
     * @return utilisateur créé
     */
    Mono<User> save(User user);

    /**
     * @param id identifiant
     * @return utilisateur ou vide
     */
    Mono<User> findById(UUID id);

    /**
     * @param email email unique
     * @return utilisateur ou vide
     */
    Mono<User> findByEmail(String email);

    /**
     * @return tous les utilisateurs
     */
    Flux<User> findAll();

    /**
     * @param user utilisateur mis à jour
     * @return utilisateur persisté
     */
    Mono<User> update(User user);
}

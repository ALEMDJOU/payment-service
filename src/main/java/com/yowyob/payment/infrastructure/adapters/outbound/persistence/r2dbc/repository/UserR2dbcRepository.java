package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.UserEntity;

import reactor.core.publisher.Mono;

/**
 * Repository Spring Data R2DBC utilisateurs.
 */
public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {

    /**
     * @param email email unique
     * @return utilisateur
     */
    Mono<UserEntity> findByEmail(String email);
}

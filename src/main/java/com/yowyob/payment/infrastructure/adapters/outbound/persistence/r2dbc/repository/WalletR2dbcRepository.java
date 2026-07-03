package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.WalletEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository Spring Data R2DBC portefeuilles.
 */
public interface WalletR2dbcRepository extends ReactiveCrudRepository<WalletEntity, UUID> {

    /**
     * @param userId propriétaire
     * @return portefeuilles
     */
    Flux<WalletEntity> findByUserId(UUID userId);

    /**
     * @param userId propriétaire
     * @return premier portefeuille
     */
    Mono<WalletEntity> findFirstByUserId(UUID userId);
}

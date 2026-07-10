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
     * @param userId         propriétaire
     * @param organizationId organisation
     * @return portefeuille
     */
    Mono<WalletEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * @param userId         propriétaire
     * @param organizationId organisation
     * @return portefeuilles filtrés
     */
    Flux<WalletEntity> findAllByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}

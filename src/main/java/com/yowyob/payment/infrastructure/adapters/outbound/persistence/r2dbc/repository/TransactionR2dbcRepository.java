package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.TransactionEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository Spring Data R2DBC transactions.
 */
public interface TransactionR2dbcRepository extends ReactiveCrudRepository<TransactionEntity, UUID> {

    /**
     * @param reference référence unique
     * @return transaction
     */
    Mono<TransactionEntity> findByReference(String reference);

    /**
     * @param walletId portefeuille
     * @return transactions
     */
    Flux<TransactionEntity> findByWalletId(UUID walletId);

    /**
     * @param userId utilisateur
     * @return transactions
     */
    Flux<TransactionEntity> findByUserId(UUID userId);

    /**
     * @param userId         utilisateur
     * @param organizationId organisation
     * @return transactions filtrées
     */
    Flux<TransactionEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}

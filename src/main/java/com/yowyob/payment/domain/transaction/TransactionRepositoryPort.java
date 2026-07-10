package com.yowyob.payment.domain.transaction;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port sortant de persistance des transactions.
 */
public interface TransactionRepositoryPort {

    /**
     * @param transaction transaction à persister
     * @return transaction créée
     */
    Mono<Transaction> save(Transaction transaction);

    /**
     * @param transaction transaction mise à jour
     * @return transaction persistée
     */
    Mono<Transaction> update(Transaction transaction);

    /**
     * @param id identifiant
     * @return transaction ou vide
     */
    Mono<Transaction> findById(UUID id);

    /**
     * @param reference référence unique
     * @return transaction ou vide
     */
    Mono<Transaction> findByReference(String reference);

    /**
     * @param walletId portefeuille
     * @return transactions du portefeuille
     */
    Flux<Transaction> findByWalletId(UUID walletId);

    /**
     * @param userId utilisateur
     * @return transactions de l'utilisateur
     */
    Flux<Transaction> findByUserId(UUID userId);

    /**
     * @param userId         utilisateur
     * @param organizationId organisation
     * @return transactions filtrées
     */
    Flux<Transaction> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}

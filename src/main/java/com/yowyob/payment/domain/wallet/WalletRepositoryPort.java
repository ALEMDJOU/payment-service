package com.yowyob.payment.domain.wallet;

import java.math.BigDecimal;
import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port sortant de persistance des portefeuilles.
 */
public interface WalletRepositoryPort {

    /**
     * @param wallet portefeuille à créer
     * @return portefeuille persisté
     */
    Mono<Wallet> save(Wallet wallet);

    /**
     * @param id identifiant
     * @return portefeuille ou vide
     */
    Mono<Wallet> findById(UUID id);

    /**
     * @param userId         propriétaire (claim sub)
     * @param organizationId organisation (claim oid)
     * @return portefeuille ou vide
     */
    Mono<Wallet> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * @param userId propriétaire
     * @return portefeuilles de l'utilisateur
     */
    Flux<Wallet> findByUserId(UUID userId);

    /**
     * @param userId         propriétaire
     * @param organizationId organisation (filtre optionnel)
     * @return portefeuilles filtrés
     */
    Flux<Wallet> findByUserIdAndOrganizationIdFilter(UUID userId, UUID organizationId);

    /**
     * @return tous les portefeuilles
     */
    Flux<Wallet> findAll();

    /**
     * @param wallet portefeuille mis à jour
     * @return portefeuille persisté
     */
    Mono<Wallet> update(Wallet wallet);

    /**
     * Débite le portefeuille de façon atomique.
     *
     * @param walletId identifiant
     * @param amount   montant à débiter (positif)
     * @return portefeuille mis à jour
     */
    Mono<Wallet> debit(UUID walletId, BigDecimal amount);

    /**
     * Crédite le portefeuille de façon atomique.
     *
     * @param walletId identifiant
     * @param amount   montant à créditer (positif)
     * @return portefeuille mis à jour
     */
    Mono<Wallet> credit(UUID walletId, BigDecimal amount);
}

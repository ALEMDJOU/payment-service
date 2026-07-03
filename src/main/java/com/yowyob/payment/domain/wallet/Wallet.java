package com.yowyob.payment.domain.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modèle domaine portefeuille.
 *
 * @param id        identifiant
 * @param userId    propriétaire
 * @param balance   solde courant
 * @param status    statut
 * @param createdAt création
 * @param updatedAt mise à jour
 */
public record Wallet(
        UUID id,
        UUID userId,
        BigDecimal balance,
        WalletStatus status,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param newBalance nouveau solde
     * @return wallet avec solde mis à jour
     */
    public Wallet withBalance(BigDecimal newBalance) {
        return new Wallet(id, userId, newBalance, status, createdAt, Instant.now());
    }

    /**
     * @param newStatus nouveau statut
     * @return wallet avec statut mis à jour
     */
    public Wallet withStatus(WalletStatus newStatus) {
        return new Wallet(id, userId, balance, newStatus, createdAt, Instant.now());
    }
}

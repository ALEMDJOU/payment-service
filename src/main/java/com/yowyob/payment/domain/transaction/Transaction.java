package com.yowyob.payment.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modèle domaine transaction.
 */
public record Transaction(
        UUID id,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String reference,
        BigDecimal fees,
        PaymentMethod method,
        String stripeSessionId,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param newStatus nouveau statut
     * @return transaction avec statut mis à jour
     */
    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(id, walletId, userId, amount, type, newStatus, reference, fees, method,
                stripeSessionId, createdAt, Instant.now());
    }

    /**
     * @param sessionId identifiant session Stripe
     * @return transaction avec session Stripe
     */
    public Transaction withStripeSessionId(String sessionId) {
        return new Transaction(id, walletId, userId, amount, type, status, reference, fees, method,
                sessionId, createdAt, Instant.now());
    }
}

package com.yowyob.payment.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Modèle domaine transaction.
 */
public record Transaction(
        UUID id,
        UUID walletId,
        UUID userId,
        UUID organizationId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String reference,
        BigDecimal fees,
        PaymentMethod method,
        String stripeSessionId,
        String callbackUrl,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @param newStatus nouveau statut
     * @return transaction avec statut mis à jour
     */
    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(id, walletId, userId, organizationId, amount, type, newStatus, reference, fees, method,
                stripeSessionId, callbackUrl, metadata, createdAt, Instant.now());
    }

    /**
     * @param sessionId identifiant session Stripe
     * @return transaction avec session Stripe
     */
    public Transaction withStripeSessionId(String sessionId) {
        return new Transaction(id, walletId, userId, organizationId, amount, type, status, reference, fees, method,
                sessionId, callbackUrl, metadata, createdAt, Instant.now());
    }
}

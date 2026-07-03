package com.yowyob.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.TransactionType;

/**
 * Événement Kafka publié lors d'un changement de statut transaction.
 */
public record TransactionEvent(
                String eventType,
                UUID transactionId,
                UUID walletId,
                BigDecimal amount,
                BigDecimal fees,
                TransactionType type,
                PaymentMethod method,
                String reference,
                Instant timestamp) {
}

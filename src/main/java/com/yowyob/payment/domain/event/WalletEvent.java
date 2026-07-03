package com.yowyob.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement Kafka publié lors d'un crédit ou débit wallet.
 */
public record WalletEvent(
                String eventType,
                UUID walletId,
                UUID userId,
                BigDecimal previousBalance,
                BigDecimal newBalance,
                BigDecimal delta,
                Instant timestamp) {
}

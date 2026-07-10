package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yowyob.payment.domain.wallet.Wallet;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO portefeuille.
 */
@Schema(description = "Portefeuille utilisateur")
public record WalletResponse(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Identifiant utilisateur (claim sub)") UUID userId,
        @Schema(description = "Identifiant organisation (claim oid)") UUID organizationId,
        @Schema(example = "1500.00") BigDecimal balance,
        @Schema(example = "ACTIVE") String status,
        @Schema(description = "Date de création") Instant createdAt,
        @Schema(description = "Date de mise à jour") Instant updatedAt) {

    /**
     * @param wallet portefeuille
     * @return DTO
     */
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.id(), wallet.userId(), wallet.organizationId(),
                wallet.balance(), wallet.status().name(), wallet.createdAt(), wallet.updatedAt());
    }
}

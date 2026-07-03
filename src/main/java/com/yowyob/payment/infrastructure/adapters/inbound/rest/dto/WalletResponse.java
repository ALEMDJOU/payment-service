package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.yowyob.payment.domain.wallet.Wallet;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO portefeuille (compat hey.json + extensions).
 */
@Schema(description = "Portefeuille utilisateur")
public record WalletResponse(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Identifiant propriétaire") UUID ownerId,
        @Schema(description = "Nom propriétaire (jointure user)") String ownerName,
        @Schema(example = "1500.00") BigDecimal balance,
        @Schema(example = "ACTIVE") String status) {
    /**
     * @param wallet    portefeuille
     * @param ownerName nom résolu
     * @return DTO
     */
    public static WalletResponse from(Wallet wallet, String ownerName) {
        return new WalletResponse(wallet.id(), wallet.userId(), ownerName, wallet.balance(),
                wallet.status().name());
    }
}

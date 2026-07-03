package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Requête création/mise à jour wallet (hey.json).
 */
@Schema(description = "Requête portefeuille")
public record WalletRequest(
                @NotNull @Schema(description = "Propriétaire") UUID ownerId,
                @Schema(description = "Nom propriétaire (informatif)") String ownerName) {
}

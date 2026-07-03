package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse solde portefeuille.
 */
@Schema(description = "Solde du portefeuille")
public record WalletBalanceResponse(
                @Schema(description = "Solde actuel", example = "1500.00") BigDecimal balance) {
}

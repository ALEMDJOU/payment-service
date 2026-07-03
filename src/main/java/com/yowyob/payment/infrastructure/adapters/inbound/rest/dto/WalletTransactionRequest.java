package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.yowyob.payment.domain.transaction.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Requête recharge ou paiement wallet.
 */
@Schema(description = "Requête transaction wallet")
public record WalletTransactionRequest(
                @NotNull @Schema(description = "Portefeuille cible", example = "550e8400-e29b-41d4-a716-446655440000") UUID walletId,
                @NotNull @Positive @Schema(description = "Montant", example = "1000.00") BigDecimal amount,
                @NotNull @Schema(description = "Méthode de paiement : WALLET (crédit immédiat) ou STRIPE (Checkout)", example = "WALLET", allowableValues = {
                                "WALLET", "STRIPE" }) PaymentMethod method) {
}

package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.yowyob.payment.domain.transaction.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Requête unifiée recharge ou paiement wallet.
 */
@Schema(description = "Transaction wallet (recharge ou paiement)")
public record TransactionRequest(
                @NotNull @Schema(example = "RECHARGE", allowableValues = {
                                "RECHARGE", "WALLET_PAYMENT" }) TransactionRequestType type,
                @NotNull @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID walletId,
                @NotNull @Positive @Schema(example = "1000.00") BigDecimal amount,
                @NotNull @Schema(example = "STRIPE", allowableValues = { "WALLET", "STRIPE" }) PaymentMethod method,
                @Schema(description = "URL HTTPS appelée lors des changements de statut (PENDING, SUCCESSED, FAILED, CANCELLED)", example = "https://merchant.example.com/webhooks/payment") String callbackUrl,
                @Size(max = 20) @Schema(description = "Métadonnées opaques renvoyées dans les webhooks consommateur") Map<String, String> metadata) {
}

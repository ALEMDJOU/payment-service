package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.yowyob.payment.domain.transaction.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Requête paiement direct (Stripe v1).
 */
@Schema(description = "Paiement direct sans wallet")
public record DirectTransactionRequest(
                @NotNull @Positive @Schema(example = "5000.00") BigDecimal amount,
                @NotNull @Schema(example = "STRIPE") PaymentMethod method,
                @Schema(description = "Utilisateur optionnel") UUID userId) {
}

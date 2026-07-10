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
 * Requête paiement direct (Stripe, client credentials).
 */
@Schema(description = "Paiement direct sans wallet")
public record DirectTransactionRequest(
                @NotNull @Positive @Schema(example = "5000.00") BigDecimal amount,
                @NotNull @Schema(example = "STRIPE") PaymentMethod method,
                @Schema(description = "Utilisateur optionnel (traçabilité)") UUID userId,
                @Schema(description = "Organisation (défaut = header X-Organization-Id)") UUID organizationId,
                @Schema(description = "URL HTTPS de notification des changements de statut", example = "https://merchant.example.com/webhooks/payment") String callbackUrl,
                @Size(max = 20) @Schema(description = "Métadonnées opaques renvoyées dans le webhook consommateur") Map<String, String> metadata) {
}

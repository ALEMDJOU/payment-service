package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload HTTP envoyé au webhook du consommateur.
 */
@Schema(description = "Notification webhook consommateur")
public record ConsumerWebhookPayload(
        @Schema(description = "Type d'événement", example = "TRANSACTION_SUCCEEDED") String eventType,
        @Schema(description = "Horodatage UTC de l'événement") Instant timestamp,
        @Schema(description = "Identifiant transaction") UUID id,
        @Schema(description = "Portefeuille associé") UUID walletId,
        @Schema(description = "Utilisateur associé") UUID userId,
        @Schema(description = "Organisation associée") UUID organizationId,
        @Schema(description = "Montant") BigDecimal amount,
        @Schema(description = "Type métier") String type,
        @Schema(description = "Statut FSM") String status,
        @Schema(description = "Référence métier") String reference,
        @Schema(description = "Frais") BigDecimal fees,
        @Schema(description = "Méthode de paiement") String method,
        @Schema(description = "Session Stripe") String stripeSessionId,
        @Schema(description = "URL Stripe Checkout (PENDING)") String stripeCheckoutUrl,
        @Schema(description = "Métadonnées opaques renvoyées telles quelles") Map<String, String> metadata,
        @Schema(description = "Date de création") Instant createdAt,
        @Schema(description = "Date de mise à jour") Instant updatedAt) {

    /**
     * @param transaction       transaction courante
     * @param eventType         type d'événement
     * @param stripeCheckoutUrl URL checkout optionnelle
     * @return payload webhook
     */
    public static ConsumerWebhookPayload from(Transaction transaction, ConsumerWebhookEventType eventType,
            String stripeCheckoutUrl) {
        return new ConsumerWebhookPayload(
                eventType.name(),
                Instant.now(),
                transaction.id(),
                transaction.walletId(),
                transaction.userId(),
                transaction.organizationId(),
                transaction.amount(),
                transaction.type().name(),
                transaction.status().name(),
                transaction.reference(),
                transaction.fees(),
                transaction.method().name(),
                transaction.stripeSessionId(),
                stripeCheckoutUrl,
                transaction.metadata(),
                transaction.createdAt(),
                transaction.updatedAt());
    }
}

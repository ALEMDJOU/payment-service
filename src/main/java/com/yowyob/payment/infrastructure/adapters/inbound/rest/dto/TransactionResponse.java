package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.yowyob.payment.application.TransactionCheckoutResult;
import com.yowyob.payment.domain.transaction.Transaction;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO transaction complète (must-have).
 */
@Schema(description = "Transaction")
public record TransactionResponse(
                @Schema(description = "Identifiant unique", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Portefeuille associé (null pour paiement direct)") UUID walletId,
                @Schema(description = "Utilisateur associé") UUID userId,
                @Schema(description = "Montant de la transaction", example = "1000.00") BigDecimal amount,
                @Schema(description = "Type de transaction", example = "RECHARGE", allowableValues = {
                                "RECHARGE",
                                "PAYMENT" }) String type,
                @Schema(description = "Statut FSM", example = "PENDING", allowableValues = { "CREATED", "PENDING",
                                "SUCCESSED", "FAILED", "CANCELLED" }) String status,
                @Schema(description = "Référence métier unique", example = "YYPAY-1234567890") String reference,
                @Schema(description = "Frais appliqués", example = "50.00") BigDecimal fees,
                @Schema(description = "Méthode de paiement", example = "STRIPE", allowableValues = { "MOMO", "PAYPAL",
                                "STRIPE", "WALLET" }) String method,
                @Schema(description = "Identifiant session Stripe Checkout") String stripeSessionId,
                @Schema(description = "URL Stripe Checkout (présente si method=STRIPE et statut PENDING)") String stripeCheckoutUrl,
                @Schema(description = "Date de création") Instant createdAt,
                @Schema(description = "Date de dernière mise à jour") Instant updatedAt) {

        /**
         * @param result résultat avec URL checkout optionnelle
         * @return DTO
         */
        public static TransactionResponse from(TransactionCheckoutResult result) {
                return from(result.transaction(), result.stripeCheckoutUrl());
        }

        /**
         * @param tx transaction domaine
         * @return DTO
         */
        public static TransactionResponse from(Transaction tx) {
                return from(tx, null);
        }

        /**
         * @param tx                transaction domaine
         * @param stripeCheckoutUrl URL checkout Stripe
         * @return DTO
         */
        public static TransactionResponse from(Transaction tx, String stripeCheckoutUrl) {
                return new TransactionResponse(tx.id(), tx.walletId(), tx.userId(), tx.amount(),
                                tx.type().name(), tx.status().name(), tx.reference(), tx.fees(),
                                tx.method().name(), tx.stripeSessionId(), stripeCheckoutUrl, tx.createdAt(),
                                tx.updatedAt());
        }
}

package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.yowyob.payment.application.StripeService;
import com.yowyob.payment.application.TransactionService;
import com.yowyob.payment.domain.exception.UserFriendlyException;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.MessageResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.TransactionResponse;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.WebhookAckResponse;
import com.yowyob.payment.infrastructure.adapters.outbound.redis.StripeIdempotencyStore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Webhooks Stripe.
 */
@Tag(name = "Stripe", description = """
        Webhooks Stripe Checkout et callbacks succès/annulation.
        Route publique — signature Stripe vérifiée sur `/webhooks`.
        """)
@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
@OpenApiStandardResponses
public class StripeController {

    private final TransactionService transactionService;
    private final StripeService stripeService;
    private final StripeIdempotencyStore idempotencyStore;

    @Value("${yowyob.stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Callback navigateur après paiement Stripe Checkout réussi.
     *
     * @param sessionId identifiant session Stripe
     * @return transaction finalisée
     */
    @GetMapping("/success")
    @Operation(summary = "Retour Stripe Checkout (succès)", description = "Route publique appelée par redirection navigateur après paiement. Finalise la transaction si le webhook n'a pas encore été traité.")
    @ApiResponse(responseCode = "200", description = "Paiement confirmé", content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public Mono<TransactionResponse> success(@RequestParam("session_id") String sessionId) {
        return stripeService.retrieveCheckoutSession(sessionId)
                .flatMap(session -> {
                    if (!"paid".equals(session.getPaymentStatus())) {
                        return Mono.error(new UserFriendlyException(
                                "Paiement Stripe non finalisé (statut: " + session.getPaymentStatus() + ")"));
                    }
                    return completeFromSessionMetadata(session);
                });
    }

    /**
     * Callback navigateur après annulation Stripe Checkout.
     *
     * @param sessionId identifiant session Stripe (optionnel)
     * @return message de confirmation
     */
    @GetMapping("/cancel")
    @Operation(summary = "Retour Stripe Checkout (annulation)", description = "Route publique appelée par redirection navigateur si l'utilisateur annule le paiement.")
    @ApiResponse(responseCode = "200", description = "Annulation enregistrée", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    public Mono<MessageResponse> cancel(@RequestParam(value = "session_id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Mono.just(new MessageResponse("Paiement annulé"));
        }
        return stripeService.retrieveCheckoutSession(sessionId)
                .flatMap(this::cancelFromSessionMetadata)
                .defaultIfEmpty(new MessageResponse("Paiement annulé"));
    }

    /**
     * @param payload   corps brut JSON Stripe
     * @param sigHeader signature Stripe
     * @return ack
     */
    @PostMapping(value = "/webhooks", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Stripe webhook endpoint", description = "Reçoit les événements Stripe (checkout.session.completed, payment_intent.*)")
    @ApiResponse(responseCode = "200", description = "Événement reçu", content = @Content(schema = @Schema(implementation = WebhookAckResponse.class)))
    public Mono<WebhookAckResponse> webhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        return Mono.fromCallable(() -> Webhook.constructEvent(payload, sigHeader, webhookSecret))
                .flatMap(this::handleEvent)
                .thenReturn(new WebhookAckResponse(true))
                .onErrorMap(SignatureVerificationException.class,
                        e -> new IllegalArgumentException("Signature Stripe invalide : " + e.getMessage()));
    }

    private Mono<Void> handleEvent(Event event) {
        return idempotencyStore.isProcessed(event.getId())
                .flatMap(processed -> {
                    if (processed) {
                        return Mono.empty();
                    }
                    Mono<Void> handled = switch (event.getType()) {
                        case "checkout.session.completed" -> onCheckoutCompleted(event);
                        case "payment_intent.succeeded" -> markSucceededFromMetadata(event);
                        case "payment_intent.payment_failed" -> markFailedFromMetadata(event);
                        default -> Mono.empty();
                    };
                    return handled.then(idempotencyStore.markProcessed(event.getId()));
                });
    }

    private Mono<Void> onCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            return Mono.empty();
        }
        return completeFromSessionMetadata(session).then();
    }

    private Mono<Void> markSucceededFromMetadata(Event event) {
        return transitionFromMetadata(event, TransactionStatus.SUCCESSED);
    }

    private Mono<Void> markFailedFromMetadata(Event event) {
        return transitionFromMetadata(event, TransactionStatus.FAILED);
    }

    private Mono<Void> transitionFromMetadata(Event event, TransactionStatus status) {
        Map<String, String> metadata = event.getDataObjectDeserializer().getObject()
                .map(obj -> {
                    if (obj instanceof com.stripe.model.PaymentIntent pi) {
                        return pi.getMetadata();
                    }
                    return Map.<String, String>of();
                }).orElse(Map.of());
        if (!metadata.containsKey("transaction_id")) {
            return Mono.empty();
        }
        UUID txId = UUID.fromString(metadata.get("transaction_id"));
        if (status == TransactionStatus.SUCCESSED) {
            return transactionService.completeStripePayment(txId).then();
        }
        return transactionService.transitionStatus(txId, status).then();
    }

    private Mono<TransactionResponse> completeFromSessionMetadata(Session session) {
        String txId = metadataValue(session, "transaction_id");
        if (txId == null || txId.isBlank()) {
            return Mono.error(new UserFriendlyException("Session Stripe sans transaction_id"));
        }
        return transactionService.completeStripePayment(UUID.fromString(txId))
                .map(TransactionResponse::from);
    }

    private Mono<MessageResponse> cancelFromSessionMetadata(Session session) {
        String txId = metadataValue(session, "transaction_id");
        if (txId == null || txId.isBlank()) {
            return Mono.empty();
        }
        return transactionService.transitionStatus(UUID.fromString(txId), TransactionStatus.CANCELLED)
                .map(tx -> new MessageResponse("Paiement annulé pour la transaction " + tx.reference()));
    }

    private String metadataValue(Session session, String key) {
        if (session.getMetadata() == null) {
            return null;
        }
        return session.getMetadata().get(key);
    }
}

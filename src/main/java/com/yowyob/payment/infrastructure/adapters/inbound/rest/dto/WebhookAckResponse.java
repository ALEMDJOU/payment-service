package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Accusé de réception webhook Stripe.
 */
@Schema(description = "Accusé de réception webhook")
public record WebhookAckResponse(
                @Schema(description = "Indique que l'événement a été reçu", example = "true") boolean received) {
}

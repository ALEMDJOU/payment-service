package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse simple avec message de confirmation.
 */
@Schema(description = "Message de confirmation")
public record MessageResponse(
                @Schema(description = "Message explicite", example = "Opération réussie") String message) {
}

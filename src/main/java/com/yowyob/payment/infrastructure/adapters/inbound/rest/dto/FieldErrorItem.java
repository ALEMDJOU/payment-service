package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Erreur de validation sur un champ.
 */
@Schema(description = "Erreur de validation sur un champ")
public record FieldErrorItem(
                @Schema(description = "Nom du champ", example = "amount") String field,
                @Schema(description = "Message d'erreur", example = "must be greater than 0") String message) {
}

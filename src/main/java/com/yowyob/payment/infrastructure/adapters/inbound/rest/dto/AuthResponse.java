package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse authentification JWT.
 */
@Schema(description = "Token JWT")
public record AuthResponse(
                @Schema(description = "Jeton Bearer") String token,
                @Schema(description = "Expiration en millisecondes", example = "86400000") long expiresIn) {
}

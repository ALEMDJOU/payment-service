package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de connexion.
 */
@Schema(description = "Connexion utilisateur")
public record LoginRequest(
                @NotBlank @Email @Schema(example = "jane@example.com") String email,
                @NotBlank @Schema(example = "password123") String password) {
}

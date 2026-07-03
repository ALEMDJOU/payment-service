package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête d'inscription.
 */
@Schema(description = "Inscription utilisateur")
public record RegisterRequest(
                @NotBlank @Schema(example = "Jane Doe") String name,
                @NotBlank @Email @Schema(example = "jane@example.com") String email,
                @NotBlank @Size(min = 8) @Schema(example = "password123") String password) {
}

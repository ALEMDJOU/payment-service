package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de mise à jour du profil utilisateur.
 */
@Schema(description = "Mise à jour du profil utilisateur")
public record UpdateProfileRequest(
                @NotBlank @Schema(description = "Nouveau nom", example = "Jane Doe") String name) {
}

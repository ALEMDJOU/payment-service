package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête de changement de mot de passe.
 */
@Schema(description = "Changement de mot de passe")
public record ChangePasswordRequest(
                @NotBlank @Schema(description = "Mot de passe actuel", example = "oldPassword123") String oldPassword,
                @NotBlank @Size(min = 8) @Schema(description = "Nouveau mot de passe", example = "newPassword456") String newPassword) {
}

package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de changement de statut utilisateur.
 */
@Schema(description = "Changement de statut utilisateur")
public record UpdateUserStatusRequest(
                @NotBlank @Schema(description = "Nouveau statut", example = "ACTIVE", allowableValues = {
                                "ACTIVE", "INACTIVE", "SUSPENDED" }) String status) {
}

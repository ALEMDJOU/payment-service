package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Requête de création d'application API.
 */
@Schema(description = "Création d'une application API")
public record ApplicationCreateRequest(
                @NotBlank @Schema(description = "Nom de l'application", example = "MonApp E-commerce") String name) {
}

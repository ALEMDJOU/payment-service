package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse application API.
 */
@Schema(description = "Application API pour paiement direct")
public record ApplicationResponse(
                @Schema(description = "Identifiant", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Nom de l'application", example = "MonApp E-commerce") String name,
                @Schema(description = "Clé API en clair (uniquement à la création)", example = "abc123...") String apiKey,
                @Schema(description = "Application active", example = "true") boolean active,
                @Schema(description = "Date de création") Instant createdAt) {
}

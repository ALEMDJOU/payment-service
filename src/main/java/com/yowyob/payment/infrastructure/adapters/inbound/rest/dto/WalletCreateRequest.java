package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Création de portefeuille (admin : userId + organizationId optionnels).
 */
@Schema(description = "Création portefeuille (body vide pour utilisateur standard)")
public record WalletCreateRequest(
        @Schema(description = "Utilisateur cible (admin uniquement, défaut = claim sub)")
        UUID userId,
        @Schema(description = "Organisation cible (admin uniquement, défaut = claim oid)")
        UUID organizationId) {
}

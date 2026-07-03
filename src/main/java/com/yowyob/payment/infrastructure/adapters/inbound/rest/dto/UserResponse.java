package com.yowyob.payment.infrastructure.adapters.inbound.rest.dto;

import java.time.Instant;
import java.util.UUID;

import com.yowyob.payment.domain.user.User;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO réponse utilisateur.
 */
@Schema(description = "Utilisateur sans mot de passe")
public record UserResponse(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(example = "Jane Doe") String name,
        @Schema(example = "jane@example.com") String email,
        @Schema(example = "ACTIVE") String status,
        @Schema(example = "USER") String role,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * @param user modèle domaine
     * @return DTO
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.status().name(),
                user.role().name(), user.createdAt(), user.updatedAt());
    }
}

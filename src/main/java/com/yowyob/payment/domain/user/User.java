package com.yowyob.payment.domain.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Modèle domaine utilisateur.
 *
 * @param id        identifiant unique
 * @param name      nom affiché
 * @param email     email unique
 * @param password  hash BCrypt (jamais exposé en API)
 * @param status    statut du compte
 * @param role      rôle applicatif
 * @param createdAt date de création
 * @param updatedAt date de mise à jour
 */
public record User(
        UUID id,
        String name,
        String email,
        String password,
        UserStatus status,
        UserRole role,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * @return copie sans mot de passe pour les réponses API
     */
    public User withoutPassword() {
        return new User(id, name, email, null, status, role, createdAt, updatedAt);
    }
}

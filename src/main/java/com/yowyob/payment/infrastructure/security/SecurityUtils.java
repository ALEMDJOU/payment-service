package com.yowyob.payment.infrastructure.security;

import java.util.UUID;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import com.yowyob.payment.domain.user.UserRole;

import reactor.core.publisher.Mono;

/**
 * Utilitaires d'accès au principal authentifié.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @return principal courant
     */
    public static Mono<AuthPrincipal> currentPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (AuthPrincipal) ctx.getAuthentication().getPrincipal());
    }

    /**
     * @return identifiant utilisateur courant
     */
    public static Mono<UUID> currentUserId() {
        return currentPrincipal().map(AuthPrincipal::getId);
    }

    /**
     * @return true si ADMIN
     */
    public static Mono<Boolean> isAdmin() {
        return currentPrincipal().map(p -> p.getRole() == UserRole.ADMIN);
    }
}

package com.yowyob.payment.infrastructure.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import reactor.core.publisher.Mono;

/**
 * Utilitaires d'accès au principal kernel authentifié.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @return principal courant
     */
    public static Mono<KernelPrincipal> currentPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (KernelPrincipal) ctx.getAuthentication().getPrincipal());
    }

    /**
     * @return identifiant utilisateur courant (claim sub)
     */
    public static Mono<UUID> currentUserId() {
        return currentPrincipal().map(KernelPrincipal::getUserId);
    }

    /**
     * @return identifiant organisation courant (claim oid)
     */
    public static Mono<UUID> currentOrganizationId() {
        return currentPrincipal().map(KernelPrincipal::getOrganizationId);
    }

    /**
     * @return identifiant tenant courant (claim tid)
     */
    public static Mono<String> currentTenantId() {
        return currentPrincipal().map(KernelPrincipal::getTenantId);
    }

    /**
     * @return permissions kernel
     */
    public static Mono<List<String>> permissions() {
        return currentPrincipal().map(KernelPrincipal::getPermissions);
    }

    /**
     * @return true si permission admin kernel
     */
    public static Mono<Boolean> isAdmin() {
        return currentPrincipal().map(KernelPrincipal::isAdmin);
    }
}

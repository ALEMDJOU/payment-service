package com.yowyob.payment.infrastructure.security;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;

import reactor.core.publisher.Mono;

/**
 * Utilitaire de test pour injecter un KernelPrincipal dans le contexte
 * de sécurité réactif — remplace @WithMockUser qui ne supporte pas les
 * principaux custom (KernelPrincipal).
 */
public final class TestSecurityContext {

    private TestSecurityContext() {}

    public static final UUID TEST_USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID TEST_ORG_ID  = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    /**
     * Construit un Mono<SecurityContext> avec un KernelPrincipal standard.
     */
    public static Mono<SecurityContext> withUser() {
        KernelPrincipal principal = new KernelPrincipal(
                TEST_USER_ID,
                "test-tenant",
                TEST_ORG_ID,
                List.of(),
                Set.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext ctx = new SecurityContextImpl(auth);
        return Mono.just(ctx);
    }
}

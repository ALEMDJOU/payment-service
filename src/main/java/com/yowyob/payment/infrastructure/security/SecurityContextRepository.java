package com.yowyob.payment.infrastructure.security;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Charge le contexte de sécurité depuis le JWT kernel RS256.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private static final String DIRECT_PATH = "/api/v1/transactions/direct";

    private final KernelJwtValidator jwtValidator;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (DIRECT_PATH.equals(path)) {
            return Mono.empty();
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.empty();
        }
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return Mono.empty();
        }

        return jwtValidator.validate(token)
                .flatMap(principal -> {
                    String headerOrgId = exchange.getRequest().getHeaders()
                            .getFirst(KernelHeaders.ORGANIZATION_ID);
                    if (headerOrgId != null && !headerOrgId.isBlank()) {
                        UUID headerOrg = parseUuid(headerOrgId);
                        if (headerOrg != null && !headerOrg.equals(principal.getOrganizationId())) {
                            return Mono.empty();
                        }
                    }
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContext context = new SecurityContextImpl(auth);
                    return Mono.just(context);
                })
                .onErrorResume(ex -> Mono.empty());
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

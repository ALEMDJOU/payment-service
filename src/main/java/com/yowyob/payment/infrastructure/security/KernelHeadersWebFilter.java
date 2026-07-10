package com.yowyob.payment.infrastructure.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.yowyob.payment.infrastructure.adapters.inbound.rest.JsonErrorWriter;
import com.yowyob.payment.infrastructure.config.KernelAuthProperties;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Valide les headers kernel obligatoires pour les routes JWT.
 */
@Component
@Order(-100)
@RequiredArgsConstructor
public class KernelHeadersWebFilter implements WebFilter {

    private static final String DIRECT_PATH = "/api/v1/transactions/direct";
    private static final String MISSING_HEADERS_MESSAGE =
            "Headers kernel requis manquants ou invalides : X-Client-Id, X-Api-Key, X-Tenant-Id, X-Organization-Id";
    private static final String ORG_MISMATCH_MESSAGE =
            "X-Organization-Id ne correspond pas au claim oid du JWT";

    private final KernelAuthProperties properties;
    private final JsonErrorWriter jsonErrorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        if (DIRECT_PATH.equals(path)) {
            return chain.filter(exchange);
        }

        String clientId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.CLIENT_ID);
        String apiKey = exchange.getRequest().getHeaders().getFirst(KernelHeaders.API_KEY);
        String tenantId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.TENANT_ID);
        String organizationId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.ORGANIZATION_ID);

        if (!matches(clientId, properties.getClientId())
                || !matches(apiKey, properties.getApiKey())
                || !matches(tenantId, properties.getTenantId())
                || organizationId == null || organizationId.isBlank()) {
            return jsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, MISSING_HEADERS_MESSAGE);
        }

        exchange.getAttributes().put(KernelHeaders.ORGANIZATION_ID, organizationId.trim());
        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/docs")
                || path.startsWith("/api/v1/stripe/");
    }

    private boolean matches(String actual, String expected) {
        return actual != null && !actual.isBlank() && actual.trim().equals(expected);
    }
}

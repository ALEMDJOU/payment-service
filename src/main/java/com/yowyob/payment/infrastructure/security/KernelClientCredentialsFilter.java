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
 * Valide les client credentials kernel pour POST /api/v1/transactions/direct.
 */
@Component
@Order(-90)
@RequiredArgsConstructor
public class KernelClientCredentialsFilter implements WebFilter {

    private static final String DIRECT_PATH = "/api/v1/transactions/direct";
    private static final String MISSING_CREDENTIALS_MESSAGE =
            "Authentification client credentials requise : X-Client-Id, X-Api-Key, X-Tenant-Id";
    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Client credentials invalides";

    private final KernelAuthProperties properties;
    private final JsonErrorWriter jsonErrorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!DIRECT_PATH.equals(path)) {
            return chain.filter(exchange);
        }

        String clientId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.CLIENT_ID);
        String apiKey = exchange.getRequest().getHeaders().getFirst(KernelHeaders.API_KEY);
        String tenantId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.TENANT_ID);

        if (clientId == null || apiKey == null || tenantId == null
                || clientId.isBlank() || apiKey.isBlank() || tenantId.isBlank()) {
            return jsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, MISSING_CREDENTIALS_MESSAGE);
        }

        if (!clientId.trim().equals(properties.getClientId())
                || !apiKey.trim().equals(properties.getApiKey())
                || !tenantId.trim().equals(properties.getTenantId())) {
            return jsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        String organizationId = exchange.getRequest().getHeaders().getFirst(KernelHeaders.ORGANIZATION_ID);
        if (organizationId != null && !organizationId.isBlank()) {
            exchange.getAttributes().put(KernelHeaders.ORGANIZATION_ID, organizationId.trim());
        }

        exchange.getAttributes().put(KernelHeaders.CLIENT_CREDENTIALS_ATTR, Boolean.TRUE);
        return chain.filter(exchange);
    }
}

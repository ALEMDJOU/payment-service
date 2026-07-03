package com.yowyob.payment.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.yowyob.payment.domain.application.ApplicationRepositoryPort;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.JsonErrorWriter;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Valide la clé API {@code X-Api-Key} pour les routes de paiement direct.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyWebFilter implements WebFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String API_KEY_AUTH_ATTR = "apiKeyAuthenticated";

    private static final String MISSING_KEY_MESSAGE = "Authentification requise : header X-Api-Key manquant pour le paiement direct";
    private static final String INVALID_KEY_MESSAGE = "Authentification échouée : clé API X-Api-Key invalide ou application inactive";

    private final ApplicationRepositoryPort applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JsonErrorWriter jsonErrorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.equals("/api/v1/transactions/direct")) {
            return chain.filter(exchange);
        }
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            return jsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, MISSING_KEY_MESSAGE);
        }
        return applicationRepository.findAllActive()
                .filter(app -> passwordEncoder.matches(apiKey, app.apiKeyHash()))
                .hasElements()
                .flatMap(valid -> {
                    if (!valid) {
                        return jsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, INVALID_KEY_MESSAGE);
                    }
                    exchange.getAttributes().put(API_KEY_AUTH_ATTR, Boolean.TRUE);
                    return chain.filter(exchange);
                });
    }
}

package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.payment.infrastructure.adapters.inbound.rest.dto.ApiErrorResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Écrit une réponse JSON d'erreur explicite dans une réponse WebFlux.
 */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    /**
     * @param exchange échange HTTP
     * @param status   statut HTTP
     * @param message  message explicite
     * @return mono terminé
     */
    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiErrorResponse body = ApiErrorResponse.of(status, message);
        return exchange.getResponse().writeWith(Mono.fromCallable(() -> serialize(body))
                .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private byte[] serialize(ApiErrorResponse body) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(body);
    }
}

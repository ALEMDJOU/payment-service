package com.yowyob.payment.infrastructure.adapters.inbound.rest;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import io.swagger.v3.oas.annotations.Hidden;
import reactor.core.publisher.Mono;

/**
 * Redirection vers le guide consommateur statique.
 */
@Hidden
@RestController
public class DocsController {

    /**
     * @param exchange échange HTTP
     * @return redirection 302 vers /docs/guide.md
     */
    @GetMapping("/docs")
    public Mono<Void> redirectToGuide(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create("/docs/guide.md"));
        return exchange.getResponse().setComplete();
    }
}

package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.domain.webhook.WebhookOutboxRepositoryPort;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.WebhookOutboxMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.WebhookOutboxR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC outbox webhook consommateur.
 */
@Component
@RequiredArgsConstructor
public class WebhookOutboxR2dbcAdapter implements WebhookOutboxRepositoryPort {

    private final WebhookOutboxR2dbcRepository repository;

    @Override
    public Mono<WebhookOutboxEntry> save(WebhookOutboxEntry entry) {
        return repository.save(WebhookOutboxMapper.toNewEntity(entry)).map(WebhookOutboxMapper::toDomain);
    }

    @Override
    public Mono<WebhookOutboxEntry> update(WebhookOutboxEntry entry) {
        return repository.save(WebhookOutboxMapper.toEntity(entry)).map(WebhookOutboxMapper::toDomain);
    }

    @Override
    public Flux<WebhookOutboxEntry> findReadyForDelivery(int limit) {
        return repository.findReadyForDelivery(limit).map(WebhookOutboxMapper::toDomain);
    }

    @Override
    public Mono<WebhookOutboxEntry> findById(UUID id) {
        return repository.findById(id).map(WebhookOutboxMapper::toDomain);
    }
}

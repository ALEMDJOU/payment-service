package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.application.ApplicationRepositoryPort;
import com.yowyob.payment.domain.application.ClientApplication;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.PersistenceMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.ApplicationR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC pour {@link ApplicationRepositoryPort}.
 */
@Component
@RequiredArgsConstructor
public class ApplicationR2dbcAdapter implements ApplicationRepositoryPort {

    private final ApplicationR2dbcRepository repository;

    @Override
    public Mono<ClientApplication> save(ClientApplication application) {
        return repository.save(PersistenceMapper.toNewEntity(application)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<ClientApplication> findAll() {
        return repository.findAll().map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<ClientApplication> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<ClientApplication> findAllActive() {
        return repository.findByActiveTrue().map(PersistenceMapper::toDomain);
    }
}

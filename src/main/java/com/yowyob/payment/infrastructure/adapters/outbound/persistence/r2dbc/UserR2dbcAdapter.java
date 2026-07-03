package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.user.UserRepositoryPort;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.PersistenceMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.UserR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC pour {@link UserRepositoryPort}.
 */
@Component
@RequiredArgsConstructor
public class UserR2dbcAdapter implements UserRepositoryPort {

    private final UserR2dbcRepository repository;

    @Override
    public Mono<User> save(User user) {
        return repository.save(PersistenceMapper.toNewEntity(user)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<User> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<User> findAll() {
        return repository.findAll().map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<User> update(User user) {
        return repository.save(PersistenceMapper.toEntity(user)).map(PersistenceMapper::toDomain);
    }
}

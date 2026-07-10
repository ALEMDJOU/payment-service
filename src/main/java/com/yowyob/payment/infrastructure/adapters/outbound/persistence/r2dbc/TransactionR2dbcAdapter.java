package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionRepositoryPort;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.PersistenceMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.TransactionR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC pour {@link TransactionRepositoryPort}.
 */
@Component
@RequiredArgsConstructor
public class TransactionR2dbcAdapter implements TransactionRepositoryPort {

    private final TransactionR2dbcRepository repository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.save(PersistenceMapper.toNewEntity(transaction)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Transaction> update(Transaction transaction) {
        return repository.save(PersistenceMapper.toEntity(transaction)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Transaction> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Transaction> findByReference(String reference) {
        return repository.findByReference(reference).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByWalletId(UUID walletId) {
        return repository.findByWalletId(walletId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByUserIdAndOrganizationId(UUID userId, UUID organizationId) {
        return repository.findByUserIdAndOrganizationId(userId, organizationId).map(PersistenceMapper::toDomain);
    }
}

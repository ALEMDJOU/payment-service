package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.yowyob.payment.domain.exception.InsufficientBalanceException;
import com.yowyob.payment.domain.exception.WalletNotFoundException;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletRepositoryPort;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.PersistenceMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.WalletR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter R2DBC pour {@link WalletRepositoryPort} avec débit/crédit atomique.
 */
@Component
@RequiredArgsConstructor
public class WalletR2dbcAdapter implements WalletRepositoryPort {

    private final WalletR2dbcRepository repository;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Wallet> save(Wallet wallet) {
        return repository.save(PersistenceMapper.toNewEntity(wallet)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Wallet> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Wallet> findByUserIdAndOrganizationId(UUID userId, UUID organizationId) {
        return repository.findByUserIdAndOrganizationId(userId, organizationId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Wallet> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Wallet> findByUserIdAndOrganizationIdFilter(UUID userId, UUID organizationId) {
        return repository.findAllByUserIdAndOrganizationId(userId, organizationId).map(PersistenceMapper::toDomain);
    }

    @Override
    public Flux<Wallet> findAll() {
        return repository.findAll().map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Wallet> update(Wallet wallet) {
        return repository.save(PersistenceMapper.toEntity(wallet)).map(PersistenceMapper::toDomain);
    }

    @Override
    public Mono<Wallet> debit(UUID walletId, BigDecimal amount) {
        return adjustBalance(walletId, amount.negate());
    }

    @Override
    public Mono<Wallet> credit(UUID walletId, BigDecimal amount) {
        return adjustBalance(walletId, amount);
    }

    private Mono<Wallet> adjustBalance(UUID walletId, BigDecimal delta) {
        Mono<Wallet> operation = databaseClient.sql(
                """
                        UPDATE "yy-pay-wallets"
                        SET balance = balance + :delta, updated_at = :now
                        WHERE id = :id AND balance + :delta >= 0
                        RETURNING id, user_id, organization_id, balance, status, created_at, updated_at
                        """)
                .bind("delta", delta)
                .bind("now", Instant.now())
                .bind("id", walletId)
                .map((row, metadata) -> new Wallet(
                        row.get("id", UUID.class),
                        row.get("user_id", UUID.class),
                        row.get("organization_id", UUID.class),
                        row.get("balance", BigDecimal.class),
                        com.yowyob.payment.domain.wallet.WalletStatus.valueOf(row.get("status", String.class)),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)))
                .one()
                .switchIfEmpty(Mono.defer(() -> repository.findById(walletId)
                        .hasElement()
                        .flatMap(exists -> exists
                                ? Mono.error(new InsufficientBalanceException("Solde insuffisant"))
                                : Mono.error(new WalletNotFoundException("Portefeuille introuvable")))));

        return transactionalOperator.transactional(operation);
    }
}

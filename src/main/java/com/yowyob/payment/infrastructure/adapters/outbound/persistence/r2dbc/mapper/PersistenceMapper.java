package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper;

import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.TransactionEntity;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.WalletEntity;
import com.yowyob.payment.infrastructure.support.JsonSupport;

/**
 * Mapping entités R2DBC vers modèles domaine.
 */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    /**
     * @param entity entité
     * @return modèle
     */
    public static Wallet toDomain(WalletEntity entity) {
        return new Wallet(entity.getId(), entity.getUserId(), entity.getOrganizationId(),
                entity.getBalance(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * @param wallet modèle
     * @return entité
     */
    public static WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder()
                .id(wallet.id())
                .userId(wallet.userId())
                .organizationId(wallet.organizationId())
                .balance(wallet.balance())
                .status(wallet.status())
                .createdAt(wallet.createdAt())
                .updatedAt(wallet.updatedAt())
                .build();
    }

    /**
     * @param wallet modèle
     * @return entité à insérer
     */
    public static WalletEntity toNewEntity(Wallet wallet) {
        WalletEntity entity = toEntity(wallet);
        entity.markNew();
        return entity;
    }

    /**
     * @param entity entité
     * @return modèle
     */
    public static Transaction toDomain(TransactionEntity entity) {
        return new Transaction(entity.getId(), entity.getWalletId(), entity.getUserId(),
                entity.getOrganizationId(), entity.getAmount(), entity.getType(), entity.getStatus(),
                entity.getReference(), entity.getFees(), entity.getMethod(), entity.getStripeSessionId(),
                entity.getCallbackUrl(), JsonSupport.readStringMap(entity.getMetadata()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * @param tx modèle
     * @return entité
     */
    public static TransactionEntity toEntity(Transaction tx) {
        return TransactionEntity.builder()
                .id(tx.id())
                .walletId(tx.walletId())
                .userId(tx.userId())
                .organizationId(tx.organizationId())
                .amount(tx.amount())
                .type(tx.type())
                .status(tx.status())
                .reference(tx.reference())
                .fees(tx.fees())
                .method(tx.method())
                .stripeSessionId(tx.stripeSessionId())
                .callbackUrl(tx.callbackUrl())
                .metadata(JsonSupport.writeStringMap(tx.metadata()))
                .createdAt(tx.createdAt())
                .updatedAt(tx.updatedAt())
                .build();
    }

    /**
     * @param tx modèle
     * @return entité à insérer
     */
    public static TransactionEntity toNewEntity(Transaction tx) {
        TransactionEntity entity = toEntity(tx);
        entity.markNew();
        return entity;
    }
}

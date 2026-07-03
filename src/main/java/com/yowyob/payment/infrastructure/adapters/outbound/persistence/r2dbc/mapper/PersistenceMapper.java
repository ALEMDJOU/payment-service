package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper;

import com.yowyob.payment.domain.application.ClientApplication;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.user.User;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.ApplicationEntity;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.TransactionEntity;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.UserEntity;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.WalletEntity;

/**
 * Mapping entités R2DBC vers modèles domaine.
 */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    /**
     * @param entity entité
     * @return modèle domaine
     */
    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getPassword(),
                entity.getStatus(), entity.getRole(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * @param user modèle
     * @return entité
     */
    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.id())
                .name(user.name())
                .email(user.email())
                .password(user.password())
                .status(user.status())
                .role(user.role())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .build();
    }

    /**
     * @param user modèle
     * @return entité à insérer
     */
    public static UserEntity toNewEntity(User user) {
        UserEntity entity = toEntity(user);
        entity.markNew();
        return entity;
    }

    /**
     * @param entity entité
     * @return modèle
     */
    public static Wallet toDomain(WalletEntity entity) {
        return new Wallet(entity.getId(), entity.getUserId(), entity.getBalance(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * @param wallet modèle
     * @return entité
     */
    public static WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder()
                .id(wallet.id())
                .userId(wallet.userId())
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
                entity.getAmount(), entity.getType(), entity.getStatus(), entity.getReference(),
                entity.getFees(), entity.getMethod(), entity.getStripeSessionId(),
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
                .amount(tx.amount())
                .type(tx.type())
                .status(tx.status())
                .reference(tx.reference())
                .fees(tx.fees())
                .method(tx.method())
                .stripeSessionId(tx.stripeSessionId())
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

    /**
     * @param entity entité
     * @return modèle
     */
    public static ClientApplication toDomain(ApplicationEntity entity) {
        return new ClientApplication(entity.getId(), entity.getName(), entity.getApiKeyHash(),
                entity.isActive(), entity.getCreatedAt());
    }

    /**
     * @param app modèle
     * @return entité
     */
    public static ApplicationEntity toEntity(ClientApplication app) {
        return ApplicationEntity.builder()
                .id(app.id())
                .name(app.name())
                .apiKeyHash(app.apiKeyHash())
                .active(app.active())
                .createdAt(app.createdAt())
                .build();
    }

    /**
     * @param app modèle
     * @return entité à insérer
     */
    public static ApplicationEntity toNewEntity(ClientApplication app) {
        ApplicationEntity entity = toEntity(app);
        entity.markNew();
        return entity;
    }
}

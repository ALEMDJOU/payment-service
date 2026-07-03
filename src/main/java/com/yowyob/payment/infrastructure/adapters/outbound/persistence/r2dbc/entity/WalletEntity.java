package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.yowyob.payment.domain.wallet.WalletStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité R2DBC portefeuille.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-wallets\"")
public class WalletEntity extends AbstractPersistableEntity<UUID> {

    @Id
    private UUID id;
    @Column("user_id")
    private UUID userId;
    private BigDecimal balance;
    private WalletStatus status;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
}

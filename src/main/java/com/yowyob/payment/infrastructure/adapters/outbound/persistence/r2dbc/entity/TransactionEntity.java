package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité R2DBC transaction.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-transactions\"")
public class TransactionEntity extends AbstractPersistableEntity<UUID> {

    @Id
    private UUID id;
    @Column("wallet_id")
    private UUID walletId;
    @Column("user_id")
    private UUID userId;
    @Column("organization_id")
    private UUID organizationId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String reference;
    private BigDecimal fees;
    private PaymentMethod method;
    @Column("stripe_session_id")
    private String stripeSessionId;
    @Column("callback_url")
    private String callbackUrl;
    @Column("metadata")
    private String metadata;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
}

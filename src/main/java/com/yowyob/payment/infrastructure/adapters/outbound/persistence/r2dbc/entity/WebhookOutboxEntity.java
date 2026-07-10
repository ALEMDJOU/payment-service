package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;
import com.yowyob.payment.domain.webhook.WebhookOutboxStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité outbox pour livraison webhook consommateur.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-webhook-outbox\"")
public class WebhookOutboxEntity extends AbstractPersistableEntity<UUID> {

    @Id
    private UUID id;
    @Column("transaction_id")
    private UUID transactionId;
    @Column("event_type")
    private ConsumerWebhookEventType eventType;
    @Column("callback_url")
    private String callbackUrl;
    private String payload;
    private WebhookOutboxStatus status;
    @Column("attempt_count")
    private int attemptCount;
    @Column("next_attempt_at")
    private Instant nextAttemptAt;
    @Column("last_error")
    private String lastError;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
}

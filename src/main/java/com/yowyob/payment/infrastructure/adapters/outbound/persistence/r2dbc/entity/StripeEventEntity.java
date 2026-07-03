package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité d'idempotence des webhooks Stripe.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-stripe-events\"")
public class StripeEventEntity extends AbstractPersistableEntity<String> {

    @Id
    @Column("event_id")
    private String eventId;
    @Column("processed_at")
    private Instant processedAt;

    @Override
    public String getId() {
        return eventId;
    }
}

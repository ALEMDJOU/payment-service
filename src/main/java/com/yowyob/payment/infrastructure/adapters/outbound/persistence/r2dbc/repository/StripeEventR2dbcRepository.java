package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.StripeEventEntity;

/**
 * Repository idempotence Stripe.
 */
public interface StripeEventR2dbcRepository extends ReactiveCrudRepository<StripeEventEntity, String> {
}

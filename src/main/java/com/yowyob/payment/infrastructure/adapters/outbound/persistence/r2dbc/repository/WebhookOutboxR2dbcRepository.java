package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.WebhookOutboxEntity;

import reactor.core.publisher.Flux;

/**
 * Repository R2DBC outbox webhook.
 */
public interface WebhookOutboxR2dbcRepository extends ReactiveCrudRepository<WebhookOutboxEntity, UUID> {

    /**
     * @param limit nombre max
     * @return entrées prêtes
     */
    @Query("""
            SELECT * FROM "yy-pay-webhook-outbox"
            WHERE status = 'PENDING' AND next_attempt_at <= NOW()
            ORDER BY next_attempt_at ASC
            LIMIT :limit
            """)
    Flux<WebhookOutboxEntity> findReadyForDelivery(int limit);
}

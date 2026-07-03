package com.yowyob.payment.infrastructure.adapters.outbound.redis;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.StripeEventEntity;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.StripeEventR2dbcRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Idempotence webhooks Stripe (Redis + PostgreSQL).
 */
@Component
@RequiredArgsConstructor
public class StripeIdempotencyStore {

        private static final String KEY_PREFIX = "stripe:processed:";

        private final ReactiveStringRedisTemplate redisTemplate;
        private final StripeEventR2dbcRepository stripeEventRepository;

        /**
         * @param eventId identifiant événement Stripe
         * @return true si déjà traité
         */
        public Mono<Boolean> isProcessed(String eventId) {
                return redisTemplate.hasKey(KEY_PREFIX + eventId)
                                .flatMap(inRedis -> inRedis ? Mono.just(true)
                                                : stripeEventRepository.existsById(eventId));
        }

        /**
         * @param eventId identifiant événement
         * @return void
         */
        public Mono<Void> markProcessed(String eventId) {
                StripeEventEntity entity = StripeEventEntity.builder()
                                .eventId(eventId)
                                .processedAt(Instant.now())
                                .build();
                entity.markNew();
                return stripeEventRepository.save(entity)
                                .then(redisTemplate.opsForValue().set(KEY_PREFIX + eventId, "1", Duration.ofDays(30)))
                                .then();
        }
}

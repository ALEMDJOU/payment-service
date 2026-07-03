package com.yowyob.payment.infrastructure.adapters.outbound.redis;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Cache Redis des soldes portefeuille.
 */
@Component
@RequiredArgsConstructor
public class WalletBalanceCache {

    private static final String KEY_PREFIX = "wallet:balance:";

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${yowyob.redis.ttl-seconds}")
    private long ttlSeconds;

    /**
     * @param walletId identifiant
     * @return solde en cache si présent
     */
    public Mono<BigDecimal> get(UUID walletId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + walletId)
                .map(BigDecimal::new);
    }

    /**
     * @param walletId identifiant
     * @param balance  solde
     * @return void
     */
    public Mono<Void> put(UUID walletId, BigDecimal balance) {
        return redisTemplate.opsForValue()
                .set(KEY_PREFIX + walletId, balance.toPlainString(), Duration.ofSeconds(ttlSeconds))
                .then();
    }

    /**
     * @param walletId identifiant
     * @return void
     */
    public Mono<Void> evict(UUID walletId) {
        return redisTemplate.delete(KEY_PREFIX + walletId).then();
    }
}

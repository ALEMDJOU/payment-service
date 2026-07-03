package com.yowyob.payment.domain.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Application cliente (clé API pour paiements directs).
 */
public record ClientApplication(
                UUID id,
                String name,
                String apiKeyHash,
                boolean active,
                Instant createdAt) {
}

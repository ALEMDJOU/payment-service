package com.yowyob.payment.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Résultat création ou listing d'une application API.
 */
public record ApplicationResult(
                UUID id,
                String name,
                String apiKey,
                boolean active,
                Instant createdAt) {
}

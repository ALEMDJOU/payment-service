package com.yowyob.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration livraison webhooks consommateurs.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "yowyob.webhook")
public class WebhookProperties {

    private int maxAttempts = 5;
    private long initialDelayMs = 1_000L;
    private double multiplier = 2.0d;
    private long pollIntervalMs = 5_000L;
    private long timeoutMs = 10_000L;
    private int batchSize = 20;
}

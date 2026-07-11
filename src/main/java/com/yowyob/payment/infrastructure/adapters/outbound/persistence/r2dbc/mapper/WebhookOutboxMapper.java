package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper;

import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.WebhookOutboxEntity;

import io.r2dbc.postgresql.codec.Json;

/**
 * Mapping outbox webhook.
 */
public final class WebhookOutboxMapper {

    private WebhookOutboxMapper() {
    }

    /**
     * @param entity entité
     * @return domaine
     */
    public static WebhookOutboxEntry toDomain(WebhookOutboxEntity entity) {
        String payloadJson = entity.getPayload() == null ? null : entity.getPayload().asString();
        return new WebhookOutboxEntry(entity.getId(), entity.getTransactionId(), entity.getEventType(),
                entity.getCallbackUrl(), payloadJson, entity.getStatus(), entity.getAttemptCount(),
                entity.getNextAttemptAt(), entity.getLastError(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    /**
     * @param entry domaine
     * @return entité
     */
    public static WebhookOutboxEntity toEntity(WebhookOutboxEntry entry) {
        return WebhookOutboxEntity.builder()
                .id(entry.id())
                .transactionId(entry.transactionId())
                .eventType(entry.eventType())
                .callbackUrl(entry.callbackUrl())
                .payload(entry.payloadJson() == null ? Json.of("{}") : Json.of(entry.payloadJson()))
                .status(entry.status())
                .attemptCount(entry.attemptCount())
                .nextAttemptAt(entry.nextAttemptAt())
                .lastError(entry.lastError())
                .createdAt(entry.createdAt())
                .updatedAt(entry.updatedAt())
                .build();
    }

    /**
     * @param entry domaine
     * @return entité nouvelle
     */
    public static WebhookOutboxEntity toNewEntity(WebhookOutboxEntry entry) {
        WebhookOutboxEntity entity = toEntity(entry);
        entity.markNew();
        return entity;
    }
}

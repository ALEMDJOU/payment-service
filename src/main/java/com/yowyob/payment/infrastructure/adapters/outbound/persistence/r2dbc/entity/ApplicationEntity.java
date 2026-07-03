package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité R2DBC application API.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-applications\"")
public class ApplicationEntity extends AbstractPersistableEntity<UUID> {

    @Id
    private UUID id;
    private String name;
    @Column("api_key_hash")
    private String apiKeyHash;
    private boolean active;
    @Column("created_at")
    private Instant createdAt;
}

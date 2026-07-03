package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.yowyob.payment.domain.user.UserRole;
import com.yowyob.payment.domain.user.UserStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité R2DBC utilisateur.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Table("\"yy-pay-users\"")
public class UserEntity extends AbstractPersistableEntity<UUID> {

    @Id
    private UUID id;
    private String name;
    private String email;
    private String password;
    private UserStatus status;
    private UserRole role;
    @Column("created_at")
    private Instant createdAt;
    @Column("updated_at")
    private Instant updatedAt;
}

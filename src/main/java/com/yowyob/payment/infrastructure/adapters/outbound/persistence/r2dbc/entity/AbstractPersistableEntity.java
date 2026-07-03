package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import lombok.Getter;
import lombok.Setter;

/**
 * Entité R2DBC avec identifiant applicatif : force insert vs update via
 * {@link Persistable#isNew()}.
 */
@Getter
@Setter
public abstract class AbstractPersistableEntity<ID> implements Persistable<ID> {

    @Transient
    private boolean newEntity;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    /**
     * Marque l'entité pour un INSERT (id déjà assigné côté domaine).
     */
    public void markNew() {
        this.newEntity = true;
    }
}

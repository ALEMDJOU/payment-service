package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.entity.ApplicationEntity;

import reactor.core.publisher.Flux;

/**
 * Repository Spring Data R2DBC applications API.
 */
public interface ApplicationR2dbcRepository extends ReactiveCrudRepository<ApplicationEntity, UUID> {

    /**
     * @return applications actives
     */
    Flux<ApplicationEntity> findByActiveTrue();
}

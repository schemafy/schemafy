package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.ConstraintColumn;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConstraintColumnRepository
        extends ReactiveCrudRepository<ConstraintColumn, String> {

    public Mono<ConstraintColumn> findByIdAndDeletedAtIsNull(String id);

    public Flux<ConstraintColumn> findByConstraintIdAndDeletedAtIsNull(String constraintId);

}

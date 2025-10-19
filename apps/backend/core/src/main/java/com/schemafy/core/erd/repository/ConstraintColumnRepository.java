package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.ConstraintColumn;

import reactor.core.publisher.Flux;

public interface ConstraintColumnRepository
        extends ReactiveCrudRepository<ConstraintColumn, String> {

    public Flux<ConstraintColumn> findByConstraintId(String constraintId);

}

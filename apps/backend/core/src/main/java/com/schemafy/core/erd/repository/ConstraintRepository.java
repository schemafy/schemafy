package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Constraint;

import reactor.core.publisher.Flux;

public interface ConstraintRepository
        extends ReactiveCrudRepository<Constraint, String> {

    public Flux<Constraint> findByTableId(String tableId);

}

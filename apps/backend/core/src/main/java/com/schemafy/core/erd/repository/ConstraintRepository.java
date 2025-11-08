package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Constraint;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConstraintRepository
        extends ReactiveCrudRepository<Constraint, String> {

    public Mono<Constraint> findByIdAndDeletedAtIsNull(String id);

    public Flux<Constraint> findByTableIdAndDeletedAtIsNull(String tableId);

}

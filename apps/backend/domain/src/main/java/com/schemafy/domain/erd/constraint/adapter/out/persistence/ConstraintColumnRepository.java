package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ConstraintColumnRepository extends ReactiveCrudRepository<ConstraintColumnEntity, String> {

  Flux<ConstraintColumnEntity> findByConstraintIdOrderBySeqNo(String constraintId);

  Flux<ConstraintColumnEntity> findByColumnId(String columnId);

  Mono<Void> deleteByConstraintId(String constraintId);

  Mono<Void> deleteByColumnId(String columnId);

}

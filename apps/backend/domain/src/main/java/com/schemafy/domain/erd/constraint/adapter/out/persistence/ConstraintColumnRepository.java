package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ConstraintColumnRepository extends ReactiveCrudRepository<ConstraintColumnEntity, String> {

  Mono<ConstraintColumnEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<ConstraintColumnEntity> findByConstraintIdAndDeletedAtIsNullOrderBySeqNo(String constraintId);

}

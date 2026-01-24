package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Constraint;

import reactor.core.publisher.Mono;

public interface GetConstraintByIdPort {

  Mono<Constraint> findConstraintById(String constraintId);

}

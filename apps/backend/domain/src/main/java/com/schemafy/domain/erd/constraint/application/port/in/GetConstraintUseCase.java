package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.erd.constraint.domain.Constraint;

import reactor.core.publisher.Mono;

public interface GetConstraintUseCase {

  Mono<Constraint> getConstraint(GetConstraintQuery query);

}

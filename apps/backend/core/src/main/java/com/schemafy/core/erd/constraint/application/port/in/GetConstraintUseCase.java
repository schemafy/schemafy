package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.erd.constraint.domain.Constraint;

import reactor.core.publisher.Mono;

public interface GetConstraintUseCase {

  Mono<Constraint> getConstraint(GetConstraintQuery query);

}

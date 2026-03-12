package com.schemafy.core.erd.constraint.application.port.out;

import com.schemafy.core.erd.constraint.domain.Constraint;

import reactor.core.publisher.Mono;

public interface CreateConstraintPort {

  Mono<Constraint> createConstraint(Constraint constraint);

}

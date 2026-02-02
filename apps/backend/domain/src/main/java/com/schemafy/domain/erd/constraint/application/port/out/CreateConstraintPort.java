package com.schemafy.domain.erd.constraint.application.port.out;

import com.schemafy.domain.erd.constraint.domain.Constraint;

import reactor.core.publisher.Mono;

public interface CreateConstraintPort {

  Mono<Constraint> createConstraint(Constraint constraint);

}

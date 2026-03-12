package com.schemafy.core.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintPort {

  Mono<Void> deleteConstraint(String constraintId);

}

package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintColumnsByConstraintIdPort {

  Mono<Void> deleteByConstraintId(String constraintId);

}

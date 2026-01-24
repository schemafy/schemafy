package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteConstraintPort {

  Mono<Void> deleteConstraint(String constraintId);

}

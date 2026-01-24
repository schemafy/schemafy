package com.schemafy.domain.erd.constraint.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteConstraintUseCase {

  Mono<Void> deleteConstraint(DeleteConstraintCommand command);

}

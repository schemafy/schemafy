package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteConstraintUseCase {

  Mono<Void> deleteConstraint(DeleteConstraintCommand command);

}

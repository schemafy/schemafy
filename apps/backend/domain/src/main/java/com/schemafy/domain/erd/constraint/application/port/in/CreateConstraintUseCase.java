package com.schemafy.domain.erd.constraint.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateConstraintUseCase {

  Mono<CreateConstraintResult> createConstraint(CreateConstraintCommand command);

}

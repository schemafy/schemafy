package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateConstraintUseCase {

  Mono<CreateConstraintResult> createConstraint(CreateConstraintCommand command);

}

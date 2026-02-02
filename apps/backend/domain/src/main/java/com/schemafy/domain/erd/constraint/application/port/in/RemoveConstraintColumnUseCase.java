package com.schemafy.domain.erd.constraint.application.port.in;

import reactor.core.publisher.Mono;

public interface RemoveConstraintColumnUseCase {

  Mono<Void> removeConstraintColumn(RemoveConstraintColumnCommand command);

}

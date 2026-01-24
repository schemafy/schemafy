package com.schemafy.domain.erd.constraint.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeConstraintColumnPositionUseCase {

  Mono<Void> changeConstraintColumnPosition(ChangeConstraintColumnPositionCommand command);

}

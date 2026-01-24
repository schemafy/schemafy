package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeConstraintColumnPositionUseCase {

  Mono<Void> changeConstraintColumnPosition(ChangeConstraintColumnPositionCommand command);

}

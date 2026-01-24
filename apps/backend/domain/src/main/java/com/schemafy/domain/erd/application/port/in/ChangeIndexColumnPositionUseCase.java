package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnPositionUseCase {

  Mono<Void> changeIndexColumnPosition(ChangeIndexColumnPositionCommand command);

}

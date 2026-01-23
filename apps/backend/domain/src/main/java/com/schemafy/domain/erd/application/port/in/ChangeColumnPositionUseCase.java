package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeColumnPositionUseCase {

  Mono<Void> changeColumnPosition(ChangeColumnPositionCommand command);

}

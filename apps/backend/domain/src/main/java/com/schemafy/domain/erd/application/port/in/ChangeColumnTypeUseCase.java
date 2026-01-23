package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeColumnTypeUseCase {

  Mono<Void> changeColumnType(ChangeColumnTypeCommand command);

}

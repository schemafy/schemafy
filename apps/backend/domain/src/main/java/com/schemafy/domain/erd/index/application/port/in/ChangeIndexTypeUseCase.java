package com.schemafy.domain.erd.index.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeIndexTypeUseCase {

  Mono<Void> changeIndexType(ChangeIndexTypeCommand command);

}

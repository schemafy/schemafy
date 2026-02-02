package com.schemafy.domain.erd.index.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeIndexNameUseCase {

  Mono<Void> changeIndexName(ChangeIndexNameCommand command);

}

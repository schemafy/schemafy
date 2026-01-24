package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeIndexNameUseCase {

  Mono<Void> changeIndexName(ChangeIndexNameCommand command);

}

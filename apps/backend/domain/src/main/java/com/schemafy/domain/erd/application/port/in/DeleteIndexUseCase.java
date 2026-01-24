package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteIndexUseCase {

  Mono<Void> deleteIndex(DeleteIndexCommand command);

}

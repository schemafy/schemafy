package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateIndexUseCase {

  Mono<CreateIndexResult> createIndex(CreateIndexCommand command);

}

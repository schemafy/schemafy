package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateSchemaUseCase {

  Mono<CreateSchemaResult> createSchema(CreateSchemaCommand command);

}

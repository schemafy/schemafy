package com.schemafy.domain.erd.schema.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteSchemaUseCase {

  Mono<Void> deleteSchema(DeleteSchemaCommand command);

}

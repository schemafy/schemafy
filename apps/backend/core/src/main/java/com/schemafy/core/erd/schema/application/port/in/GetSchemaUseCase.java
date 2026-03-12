package com.schemafy.core.erd.schema.application.port.in;

import com.schemafy.core.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;

public interface GetSchemaUseCase {

  Mono<Schema> getSchema(GetSchemaQuery query);

}

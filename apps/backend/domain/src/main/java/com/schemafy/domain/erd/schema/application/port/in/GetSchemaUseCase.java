package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;

public interface GetSchemaUseCase {

  Mono<Schema> getSchema(GetSchemaQuery query);

}

package com.schemafy.core.erd.schema.application.port.in;

import reactor.core.publisher.Mono;

public interface GetSchemaWithRevisionUseCase {

  Mono<GetSchemaWithRevisionResult> getSchemaWithRevision(GetSchemaQuery query);

}

package com.schemafy.core.erd.schema.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteSchemaUseCase {

  Mono<MutationResult<Void>> deleteSchema(DeleteSchemaCommand command);

}

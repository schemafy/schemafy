package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteSchemaUseCase {

  Mono<MutationResult<Void>> deleteSchema(DeleteSchemaCommand command);

}

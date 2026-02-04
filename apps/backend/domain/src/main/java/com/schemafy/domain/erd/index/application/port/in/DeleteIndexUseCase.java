package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteIndexUseCase {

  Mono<MutationResult<Void>> deleteIndex(DeleteIndexCommand command);

}

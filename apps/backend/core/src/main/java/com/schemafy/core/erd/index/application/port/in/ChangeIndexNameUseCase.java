package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeIndexNameUseCase {

  Mono<MutationResult<Void>> changeIndexName(ChangeIndexNameCommand command);

}

package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeIndexTypeUseCase {

  Mono<MutationResult<Void>> changeIndexType(ChangeIndexTypeCommand command);

}

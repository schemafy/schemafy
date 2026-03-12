package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnPositionUseCase {

  Mono<MutationResult<Void>> changeIndexColumnPosition(
      ChangeIndexColumnPositionCommand command);

}

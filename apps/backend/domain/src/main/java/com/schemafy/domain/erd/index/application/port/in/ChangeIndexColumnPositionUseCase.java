package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnPositionUseCase {

  Mono<MutationResult<Void>> changeIndexColumnPosition(
      ChangeIndexColumnPositionCommand command);

}

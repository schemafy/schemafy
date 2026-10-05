package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTablePositionUseCase {

  Mono<MutationResult<Void>> changeTablePosition(ChangeTablePositionCommand command);

}

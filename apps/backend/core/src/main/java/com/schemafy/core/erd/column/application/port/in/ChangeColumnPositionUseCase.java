package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnPositionUseCase {

  Mono<MutationResult<Void>> changeColumnPosition(ChangeColumnPositionCommand command);

}

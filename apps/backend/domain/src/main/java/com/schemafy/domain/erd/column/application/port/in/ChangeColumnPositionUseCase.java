package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnPositionUseCase {

  Mono<MutationResult<Void>> changeColumnPosition(ChangeColumnPositionCommand command);

}

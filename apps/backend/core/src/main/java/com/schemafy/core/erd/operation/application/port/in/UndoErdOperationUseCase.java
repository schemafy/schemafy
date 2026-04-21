package com.schemafy.core.erd.operation.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface UndoErdOperationUseCase {

  Mono<MutationResult<Void>> undo(UndoErdOperationCommand command);

}

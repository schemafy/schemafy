package com.schemafy.core.erd.operation.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface RedoErdOperationUseCase {

  Mono<MutationResult<Void>> redo(RedoErdOperationCommand command);

}

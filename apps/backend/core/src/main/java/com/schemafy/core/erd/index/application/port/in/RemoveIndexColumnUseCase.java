package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface RemoveIndexColumnUseCase {

  Mono<MutationResult<Void>> removeIndexColumn(RemoveIndexColumnCommand command);

}

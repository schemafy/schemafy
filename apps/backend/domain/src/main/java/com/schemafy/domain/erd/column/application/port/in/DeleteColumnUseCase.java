package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteColumnUseCase {

  Mono<MutationResult<Void>> deleteColumn(DeleteColumnCommand command);

}

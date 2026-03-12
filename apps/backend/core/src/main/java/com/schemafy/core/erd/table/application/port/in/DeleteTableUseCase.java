package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteTableUseCase {

  Mono<MutationResult<Void>> deleteTable(DeleteTableCommand command);

}

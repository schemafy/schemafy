package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteTableUseCase {

  Mono<MutationResult<Void>> deleteTable(DeleteTableCommand command);

}

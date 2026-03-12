package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface CreateTableUseCase {

  Mono<MutationResult<CreateTableResult>> createTable(CreateTableCommand command);

}

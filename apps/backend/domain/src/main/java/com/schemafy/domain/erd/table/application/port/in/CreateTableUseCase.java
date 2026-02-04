package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface CreateTableUseCase {

  Mono<MutationResult<CreateTableResult>> createTable(CreateTableCommand command);

}

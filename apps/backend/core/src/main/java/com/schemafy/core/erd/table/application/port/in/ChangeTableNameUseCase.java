package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableNameUseCase {

  Mono<MutationResult<Void>> changeTableName(ChangeTableNameCommand command);

}

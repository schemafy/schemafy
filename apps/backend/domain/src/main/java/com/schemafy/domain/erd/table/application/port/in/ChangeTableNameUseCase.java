package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableNameUseCase {

  Mono<MutationResult<Void>> changeTableName(ChangeTableNameCommand command);

}

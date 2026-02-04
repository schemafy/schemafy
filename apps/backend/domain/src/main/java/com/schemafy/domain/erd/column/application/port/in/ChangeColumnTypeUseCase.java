package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnTypeUseCase {

  Mono<MutationResult<Void>> changeColumnType(ChangeColumnTypeCommand command);

}

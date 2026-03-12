package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnNameUseCase {

  Mono<MutationResult<Void>> changeColumnName(ChangeColumnNameCommand command);

}

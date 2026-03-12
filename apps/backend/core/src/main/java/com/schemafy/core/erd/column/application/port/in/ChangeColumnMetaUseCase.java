package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnMetaUseCase {

  Mono<MutationResult<Void>> changeColumnMeta(ChangeColumnMetaCommand command);

}

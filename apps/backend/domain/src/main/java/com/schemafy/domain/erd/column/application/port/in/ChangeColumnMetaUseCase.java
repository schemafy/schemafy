package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeColumnMetaUseCase {

  Mono<MutationResult<Void>> changeColumnMeta(ChangeColumnMetaCommand command);

}

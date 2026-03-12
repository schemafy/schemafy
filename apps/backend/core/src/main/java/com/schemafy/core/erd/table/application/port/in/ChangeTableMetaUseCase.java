package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableMetaUseCase {

  Mono<MutationResult<Void>> changeTableMeta(ChangeTableMetaCommand command);

}

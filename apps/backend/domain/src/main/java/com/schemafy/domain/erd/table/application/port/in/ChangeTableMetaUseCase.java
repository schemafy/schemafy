package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableMetaUseCase {

  Mono<MutationResult<Void>> changeTableMeta(ChangeTableMetaCommand command);

}

package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableExtraUseCase {

  Mono<MutationResult<Void>> changeTableExtra(ChangeTableExtraCommand command);

}

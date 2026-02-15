package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeTableExtraUseCase {

  Mono<MutationResult<Void>> changeTableExtra(ChangeTableExtraCommand command);

}

package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface AddIndexColumnUseCase {

  Mono<MutationResult<AddIndexColumnResult>> addIndexColumn(AddIndexColumnCommand command);

}

package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface AddIndexColumnUseCase {

  Mono<MutationResult<AddIndexColumnResult>> addIndexColumn(AddIndexColumnCommand command);

}

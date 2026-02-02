package com.schemafy.domain.erd.index.application.port.in;

import reactor.core.publisher.Mono;

public interface AddIndexColumnUseCase {

  Mono<AddIndexColumnResult> addIndexColumn(AddIndexColumnCommand command);

}

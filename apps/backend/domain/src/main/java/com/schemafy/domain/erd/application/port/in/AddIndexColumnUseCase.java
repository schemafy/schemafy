package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface AddIndexColumnUseCase {

  Mono<AddIndexColumnResult> addIndexColumn(AddIndexColumnCommand command);

}

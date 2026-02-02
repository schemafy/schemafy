package com.schemafy.domain.erd.index.application.port.in;

import reactor.core.publisher.Mono;

public interface RemoveIndexColumnUseCase {

  Mono<Void> removeIndexColumn(RemoveIndexColumnCommand command);

}

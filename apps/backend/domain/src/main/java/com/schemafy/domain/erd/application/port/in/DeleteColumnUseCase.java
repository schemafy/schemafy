package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteColumnUseCase {

  Mono<Void> deleteColumn(DeleteColumnCommand command);

}

package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateColumnUseCase {

  Mono<CreateColumnResult> createColumn(CreateColumnCommand command);

}

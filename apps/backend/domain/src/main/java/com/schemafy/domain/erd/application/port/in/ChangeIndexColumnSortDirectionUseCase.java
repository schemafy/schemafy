package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeIndexColumnSortDirectionUseCase {

  Mono<Void> changeIndexColumnSortDirection(ChangeIndexColumnSortDirectionCommand command);

}

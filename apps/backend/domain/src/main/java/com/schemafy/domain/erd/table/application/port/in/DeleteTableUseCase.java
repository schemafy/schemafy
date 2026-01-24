package com.schemafy.domain.erd.table.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteTableUseCase {

  Mono<Void> deleteTable(DeleteTableCommand command);

}

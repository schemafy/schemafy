package com.schemafy.domain.erd.table.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateTableUseCase {

  public Mono<CreateTableResult> createTable(CreateTableCommand command);

}

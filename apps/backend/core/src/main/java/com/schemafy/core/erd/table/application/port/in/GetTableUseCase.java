package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;

public interface GetTableUseCase {

  Mono<Table> getTable(GetTableQuery query);

}

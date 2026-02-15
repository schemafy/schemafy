package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Mono;

public interface GetTableUseCase {

  Mono<Table> getTable(GetTableQuery query);

}

package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;

public interface GetTablesBySchemaIdUseCase {

  Flux<Table> getTablesBySchemaId(GetTablesBySchemaIdQuery query);

}

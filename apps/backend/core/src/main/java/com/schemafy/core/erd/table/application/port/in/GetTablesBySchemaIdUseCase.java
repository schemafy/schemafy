package com.schemafy.core.erd.table.application.port.in;

import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Flux;

public interface GetTablesBySchemaIdUseCase {

  Flux<Table> getTablesBySchemaId(GetTablesBySchemaIdQuery query);

}

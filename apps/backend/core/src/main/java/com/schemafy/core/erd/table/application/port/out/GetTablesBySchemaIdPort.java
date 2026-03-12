package com.schemafy.core.erd.table.application.port.out;

import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Flux;

public interface GetTablesBySchemaIdPort {

  Flux<Table> findTablesBySchemaId(String schemaId);

}

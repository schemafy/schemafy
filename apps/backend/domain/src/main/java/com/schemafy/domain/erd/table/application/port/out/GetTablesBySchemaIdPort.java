package com.schemafy.domain.erd.table.application.port.out;

import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Flux;

public interface GetTablesBySchemaIdPort {

  Flux<Table> findTablesBySchemaId(String schemaId);

}

package com.schemafy.core.erd.table.application.port.out;

import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;

public interface GetTableByIdPort {

  Mono<Table> findTableById(String tableId);

}

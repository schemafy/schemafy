package com.schemafy.domain.erd.table.application.port.out;

import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Mono;

public interface CreateTablePort {

  Mono<Table> createTable(Table table);

}

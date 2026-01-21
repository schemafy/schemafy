package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Table;

import reactor.core.publisher.Mono;

public interface CreateTablePort {

  Mono<Table> createTable(Table table);

}

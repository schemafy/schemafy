package com.schemafy.core.erd.column.application.port.out;

import com.schemafy.core.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface CreateColumnPort {

  Mono<Column> createColumn(Column column);

}

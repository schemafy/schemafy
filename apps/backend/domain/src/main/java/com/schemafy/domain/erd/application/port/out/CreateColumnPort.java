package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Column;

import reactor.core.publisher.Mono;

public interface CreateColumnPort {

  Mono<Column> createColumn(Column column);

}

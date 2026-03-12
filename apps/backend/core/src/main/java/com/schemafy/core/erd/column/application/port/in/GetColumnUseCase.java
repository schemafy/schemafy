package com.schemafy.core.erd.column.application.port.in;

import com.schemafy.core.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnUseCase {

  Mono<Column> getColumn(GetColumnQuery query);

}

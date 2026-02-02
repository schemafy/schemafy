package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnUseCase {

  Mono<Column> getColumn(GetColumnQuery query);

}

package com.schemafy.domain.erd.column.application.port.out;

import com.schemafy.domain.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnByIdPort {

  Mono<Column> findColumnById(String columnId);

}

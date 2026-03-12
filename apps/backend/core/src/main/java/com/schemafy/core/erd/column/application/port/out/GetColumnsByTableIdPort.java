package com.schemafy.core.erd.column.application.port.out;

import java.util.List;

import com.schemafy.core.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnsByTableIdPort {

  Mono<List<Column>> findColumnsByTableId(String tableId);

}

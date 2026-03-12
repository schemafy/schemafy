package com.schemafy.core.erd.column.application.port.in;

import java.util.List;

import com.schemafy.core.erd.column.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnsByTableIdUseCase {

  Mono<List<Column>> getColumnsByTableId(GetColumnsByTableIdQuery query);

}

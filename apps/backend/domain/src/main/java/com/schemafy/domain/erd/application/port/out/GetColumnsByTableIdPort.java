package com.schemafy.domain.erd.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.domain.Column;

import reactor.core.publisher.Mono;

public interface GetColumnsByTableIdPort {

  Mono<List<Column>> findColumnsByTableId(String tableId);

}

package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnUseCase {

  Mono<IndexColumn> getIndexColumn(GetIndexColumnQuery query);

}

package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnUseCase {

  Mono<IndexColumn> getIndexColumn(GetIndexColumnQuery query);

}

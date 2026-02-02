package com.schemafy.domain.erd.index.application.port.in;

import java.util.List;

import com.schemafy.domain.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnsByIndexIdUseCase {

  Mono<List<IndexColumn>> getIndexColumnsByIndexId(GetIndexColumnsByIndexIdQuery query);

}

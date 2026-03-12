package com.schemafy.core.erd.index.application.port.out;

import java.util.List;

import com.schemafy.core.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnsByIndexIdPort {

  Mono<List<IndexColumn>> findIndexColumnsByIndexId(String indexId);

}

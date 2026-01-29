package com.schemafy.domain.erd.index.application.port.out;

import java.util.List;

import com.schemafy.domain.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnsByColumnIdPort {

  Mono<List<IndexColumn>> findIndexColumnsByColumnId(String columnId);

}

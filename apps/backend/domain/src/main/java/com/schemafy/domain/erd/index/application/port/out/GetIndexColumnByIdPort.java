package com.schemafy.domain.erd.index.application.port.out;

import com.schemafy.domain.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnByIdPort {

  Mono<IndexColumn> findIndexColumnById(String indexColumnId);

}

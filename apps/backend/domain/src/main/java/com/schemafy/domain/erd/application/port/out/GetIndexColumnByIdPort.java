package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface GetIndexColumnByIdPort {

  Mono<IndexColumn> findIndexColumnById(String indexColumnId);

}

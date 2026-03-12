package com.schemafy.core.erd.index.application.port.out;

import com.schemafy.core.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface CreateIndexColumnPort {

  Mono<IndexColumn> createIndexColumn(IndexColumn indexColumn);

}

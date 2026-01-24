package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface CreateIndexColumnPort {

  Mono<IndexColumn> createIndexColumn(IndexColumn indexColumn);

}

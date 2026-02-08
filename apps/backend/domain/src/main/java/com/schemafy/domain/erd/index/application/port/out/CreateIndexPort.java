package com.schemafy.domain.erd.index.application.port.out;

import com.schemafy.domain.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface CreateIndexPort {

  Mono<Index> createIndex(Index index);

}

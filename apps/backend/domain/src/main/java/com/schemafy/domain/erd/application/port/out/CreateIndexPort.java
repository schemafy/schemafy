package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Index;

import reactor.core.publisher.Mono;

public interface CreateIndexPort {

  Mono<Index> createIndex(Index index);

}

package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexUseCase {

  Mono<Index> getIndex(GetIndexQuery query);

}

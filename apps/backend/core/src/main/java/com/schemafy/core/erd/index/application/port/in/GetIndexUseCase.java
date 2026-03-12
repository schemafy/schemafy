package com.schemafy.core.erd.index.application.port.in;

import com.schemafy.core.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexUseCase {

  Mono<Index> getIndex(GetIndexQuery query);

}

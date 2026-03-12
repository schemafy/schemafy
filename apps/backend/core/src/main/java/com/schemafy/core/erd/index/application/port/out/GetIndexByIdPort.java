package com.schemafy.core.erd.index.application.port.out;

import com.schemafy.core.erd.index.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexByIdPort {

  Mono<Index> findIndexById(String indexId);

}

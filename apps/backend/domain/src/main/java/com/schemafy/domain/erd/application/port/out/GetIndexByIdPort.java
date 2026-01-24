package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Index;

import reactor.core.publisher.Mono;

public interface GetIndexByIdPort {

  Mono<Index> findIndexById(String indexId);

}

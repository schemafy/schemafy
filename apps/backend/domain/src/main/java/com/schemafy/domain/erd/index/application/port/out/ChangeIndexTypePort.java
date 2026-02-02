package com.schemafy.domain.erd.index.application.port.out;

import com.schemafy.domain.erd.index.domain.type.IndexType;

import reactor.core.publisher.Mono;

public interface ChangeIndexTypePort {

  Mono<Void> changeIndexType(String indexId, IndexType type);

}

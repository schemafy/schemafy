package com.schemafy.core.erd.index.application.port.out;

import com.schemafy.core.erd.index.domain.type.IndexType;

import reactor.core.publisher.Mono;

public interface ChangeIndexTypePort {

  Mono<Void> changeIndexType(String indexId, IndexType type);

}

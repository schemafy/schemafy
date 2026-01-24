package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.type.IndexType;

import reactor.core.publisher.Mono;

public interface ChangeIndexTypePort {

  Mono<Void> changeIndexType(String indexId, IndexType type);

}

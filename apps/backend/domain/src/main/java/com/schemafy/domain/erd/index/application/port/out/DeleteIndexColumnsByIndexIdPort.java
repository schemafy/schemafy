package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteIndexColumnsByIndexIdPort {

  Mono<Void> deleteByIndexId(String indexId);

}

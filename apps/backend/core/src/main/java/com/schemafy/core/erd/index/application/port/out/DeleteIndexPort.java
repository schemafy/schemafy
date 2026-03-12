package com.schemafy.core.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteIndexPort {

  Mono<Void> deleteIndex(String indexId);

}

package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteIndexPort {

  Mono<Void> deleteIndex(String indexId);

}

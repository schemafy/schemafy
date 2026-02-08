package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeIndexNamePort {

  Mono<Void> changeIndexName(String indexId, String newName);

}

package com.schemafy.core.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeIndexNamePort {

  Mono<Void> changeIndexName(String indexId, String newName);

}

package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface CascadeDeleteIndexesByTableIdPort {

  Mono<Void> cascadeDeleteByTableId(String tableId);

}

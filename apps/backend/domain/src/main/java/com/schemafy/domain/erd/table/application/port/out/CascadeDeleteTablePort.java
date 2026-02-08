package com.schemafy.domain.erd.table.application.port.out;

import reactor.core.publisher.Mono;

public interface CascadeDeleteTablePort {

  Mono<Void> cascadeDelete(String tableId);

}

package com.schemafy.domain.erd.table.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteTablePort {

  Mono<Void> deleteTable(String tableId);

}

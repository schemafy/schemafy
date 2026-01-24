package com.schemafy.domain.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteColumnsByTableIdPort {

  Mono<Void> deleteColumnsByTableId(String tableId);

}

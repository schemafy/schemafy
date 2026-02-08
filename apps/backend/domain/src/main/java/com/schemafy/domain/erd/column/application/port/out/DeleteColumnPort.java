package com.schemafy.domain.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteColumnPort {

  Mono<Void> deleteColumn(String columnId);

}

package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteIndexColumnsByColumnIdPort {

  Mono<Void> deleteByColumnId(String columnId);

}

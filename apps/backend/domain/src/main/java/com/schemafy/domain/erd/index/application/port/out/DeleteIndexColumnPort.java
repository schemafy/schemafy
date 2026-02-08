package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteIndexColumnPort {

  Mono<Void> deleteIndexColumn(String indexColumnId);

}

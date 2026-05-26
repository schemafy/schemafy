package com.schemafy.core.erd.index.application.port.out;

import com.schemafy.core.erd.index.domain.IndexColumn;

import reactor.core.publisher.Mono;

public interface RestoreIndexColumnPort {

  Mono<Void> restoreIndexColumn(IndexColumn indexColumn);

}

package com.schemafy.domain.erd.table.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeTableMetaUseCase {

  Mono<Void> changeTableMeta(ChangeTableMetaCommand command);

}

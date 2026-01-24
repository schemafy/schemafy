package com.schemafy.domain.erd.column.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeColumnMetaUseCase {

  Mono<Void> changeColumnMeta(ChangeColumnMetaCommand command);

}

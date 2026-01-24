package com.schemafy.domain.erd.table.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeTableExtraUseCase {

  Mono<Void> changeTableExtra(ChangeTableExtraCommand command);

}

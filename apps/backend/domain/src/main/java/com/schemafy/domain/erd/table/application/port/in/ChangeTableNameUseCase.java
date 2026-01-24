package com.schemafy.domain.erd.table.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeTableNameUseCase {

  Mono<Void> changeTableName(ChangeTableNameCommand command);

}

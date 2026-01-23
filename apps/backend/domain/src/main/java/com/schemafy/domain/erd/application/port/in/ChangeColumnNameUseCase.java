package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeColumnNameUseCase {

  Mono<Void> changeColumnName(ChangeColumnNameCommand command);

}

package com.schemafy.domain.erd.schema.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeSchemaNameUseCase {

  Mono<Void> changeSchemaName(ChangeSchemaNameCommand command);

}

package com.schemafy.domain.erd.schema.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeSchemaNamePort {

  Mono<Void> changeSchemaName(String schemaId, String newName);

}

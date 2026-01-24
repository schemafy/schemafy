package com.schemafy.domain.erd.schema.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteSchemaPort {

  Mono<Void> deleteSchema(String schemaId);

}

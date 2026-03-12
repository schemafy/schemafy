package com.schemafy.core.erd.schema.application.port.out;

import com.schemafy.core.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;

public interface GetSchemaByIdPort {

  Mono<Schema> findSchemaById(String schemaId);

}

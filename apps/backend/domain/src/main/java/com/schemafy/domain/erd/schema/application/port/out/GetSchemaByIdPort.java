package com.schemafy.domain.erd.schema.application.port.out;

import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;

public interface GetSchemaByIdPort {

  Mono<Schema> findSchemaById(String schemaId);

}

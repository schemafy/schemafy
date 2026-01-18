package com.schemafy.domain.erd.application.port.out;

import com.schemafy.domain.erd.domain.Schema;

import reactor.core.publisher.Mono;

public interface ReactiveCreateSchema {

  Mono<Schema> createSchema(Schema schema);

}

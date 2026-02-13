package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Flux;

public interface GetSchemasByProjectIdUseCase {

  Flux<Schema> getSchemasByProjectId(GetSchemasByProjectIdQuery query);

}

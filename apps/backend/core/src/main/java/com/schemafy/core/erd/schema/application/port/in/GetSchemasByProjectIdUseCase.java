package com.schemafy.core.erd.schema.application.port.in;

import com.schemafy.core.erd.schema.domain.Schema;

import reactor.core.publisher.Flux;

public interface GetSchemasByProjectIdUseCase {

  Flux<Schema> getSchemasByProjectId(GetSchemasByProjectIdQuery query);

}

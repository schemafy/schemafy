package com.schemafy.core.erd.schema.application.port.out;

import com.schemafy.core.erd.schema.domain.Schema;

import reactor.core.publisher.Flux;

public interface GetSchemasByProjectIdPort {

  Flux<Schema> findSchemasByProjectId(String projectId);

}

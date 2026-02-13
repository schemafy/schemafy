package com.schemafy.domain.erd.schema.application.port.out;

import com.schemafy.domain.erd.schema.domain.Schema;

import reactor.core.publisher.Flux;

public interface GetSchemasByProjectIdPort {

  Flux<Schema> findSchemasByProjectId(String projectId);

}

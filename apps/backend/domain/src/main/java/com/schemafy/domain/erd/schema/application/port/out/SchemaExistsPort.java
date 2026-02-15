package com.schemafy.domain.erd.schema.application.port.out;

import reactor.core.publisher.Mono;

public interface SchemaExistsPort {

  Mono<Boolean> existsActiveByProjectIdAndName(String projectId, String name);

}

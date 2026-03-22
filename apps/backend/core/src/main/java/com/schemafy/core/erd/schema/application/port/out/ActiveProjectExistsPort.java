package com.schemafy.core.erd.schema.application.port.out;

import reactor.core.publisher.Mono;

public interface ActiveProjectExistsPort {

  Mono<Boolean> existsActiveProjectById(String projectId);

}

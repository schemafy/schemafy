package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteProjectUseCase {

  Mono<Void> deleteProject(DeleteProjectCommand command);

}

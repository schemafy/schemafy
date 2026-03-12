package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface UpdateProjectUseCase {

  Mono<ProjectDetail> updateProject(UpdateProjectCommand command);

}

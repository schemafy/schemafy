package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateProjectUseCase {

  Mono<ProjectDetail> createProject(CreateProjectCommand command);

}

package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface GetProjectUseCase {

  Mono<ProjectDetail> getProject(GetProjectQuery query);

}

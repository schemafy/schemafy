package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;

import reactor.core.publisher.Mono;

public interface GetProjectsUseCase {

  Mono<PageResult<ProjectSummary>> getProjects(GetProjectsQuery query);

}

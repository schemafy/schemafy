package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.Workspace;

import reactor.core.publisher.Mono;

public interface GetWorkspacesUseCase {

  Mono<PageResult<Workspace>> getWorkspaces(GetWorkspacesQuery query);

}

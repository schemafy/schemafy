package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.domain.Workspace;

import reactor.core.publisher.Mono;

public interface GetWorkspacesUseCase {

  Mono<PageResult<Workspace>> getWorkspaces(GetWorkspacesQuery query);

}

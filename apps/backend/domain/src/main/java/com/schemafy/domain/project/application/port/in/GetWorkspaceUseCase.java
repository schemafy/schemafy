package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface GetWorkspaceUseCase {

  Mono<WorkspaceDetail> getWorkspace(GetWorkspaceQuery query);

}

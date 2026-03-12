package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateWorkspaceUseCase {

  Mono<WorkspaceDetail> createWorkspace(CreateWorkspaceCommand command);

}

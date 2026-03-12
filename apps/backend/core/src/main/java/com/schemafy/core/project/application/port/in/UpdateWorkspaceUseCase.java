package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface UpdateWorkspaceUseCase {

  Mono<WorkspaceDetail> updateWorkspace(UpdateWorkspaceCommand command);

}

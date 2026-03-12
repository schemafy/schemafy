package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteWorkspaceUseCase {

  Mono<Void> deleteWorkspace(DeleteWorkspaceCommand command);

}

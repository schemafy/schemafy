package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface LeaveWorkspaceUseCase {

  Mono<Void> leaveWorkspace(LeaveWorkspaceCommand command);

}

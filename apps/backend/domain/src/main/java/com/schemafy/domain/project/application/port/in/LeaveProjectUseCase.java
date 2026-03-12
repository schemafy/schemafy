package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface LeaveProjectUseCase {

  Mono<Void> leaveProject(LeaveProjectCommand command);

}

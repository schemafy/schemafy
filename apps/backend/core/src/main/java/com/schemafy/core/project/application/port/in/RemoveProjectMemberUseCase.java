package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface RemoveProjectMemberUseCase {

  Mono<Void> removeProjectMember(RemoveProjectMemberCommand command);

}

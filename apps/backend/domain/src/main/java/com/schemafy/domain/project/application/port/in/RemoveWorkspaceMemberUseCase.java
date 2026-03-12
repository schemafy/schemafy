package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface RemoveWorkspaceMemberUseCase {

  Mono<Void> removeWorkspaceMember(RemoveWorkspaceMemberCommand command);

}

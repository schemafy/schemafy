package com.schemafy.core.project.application.port.in;

import reactor.core.publisher.Mono;

public interface RejectProjectInvitationUseCase {

  Mono<Void> rejectProjectInvitation(RejectProjectInvitationCommand command);

}

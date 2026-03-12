package com.schemafy.domain.project.application.port.in;

import reactor.core.publisher.Mono;

public interface RejectWorkspaceInvitationUseCase {

  Mono<Void> rejectWorkspaceInvitation(RejectWorkspaceInvitationCommand command);

}

package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface CreateProjectInvitationUseCase {

  Mono<Invitation> createProjectInvitation(CreateProjectInvitationCommand command);

}

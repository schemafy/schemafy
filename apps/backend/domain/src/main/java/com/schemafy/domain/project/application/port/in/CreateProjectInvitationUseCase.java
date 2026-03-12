package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface CreateProjectInvitationUseCase {

  Mono<Invitation> createProjectInvitation(CreateProjectInvitationCommand command);

}

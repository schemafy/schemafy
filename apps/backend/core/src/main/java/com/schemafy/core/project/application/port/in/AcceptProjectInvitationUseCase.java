package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Mono;

public interface AcceptProjectInvitationUseCase {

  Mono<ProjectMember> acceptProjectInvitation(AcceptProjectInvitationCommand command);

}

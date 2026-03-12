package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface AcceptWorkspaceInvitationUseCase {

  Mono<WorkspaceMember> acceptWorkspaceInvitation(
      AcceptWorkspaceInvitationCommand command);

}

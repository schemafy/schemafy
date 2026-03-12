package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface AcceptWorkspaceInvitationUseCase {

  Mono<WorkspaceMember> acceptWorkspaceInvitation(
      AcceptWorkspaceInvitationCommand command);

}

package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface GetWorkspaceInvitationsUseCase {

  Mono<PageResult<Invitation>> getWorkspaceInvitations(
      GetWorkspaceInvitationsQuery query);

}

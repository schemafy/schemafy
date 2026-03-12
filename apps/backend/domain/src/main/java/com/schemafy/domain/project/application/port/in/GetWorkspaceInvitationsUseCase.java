package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface GetWorkspaceInvitationsUseCase {

  Mono<PageResult<Invitation>> getWorkspaceInvitations(
      GetWorkspaceInvitationsQuery query);

}

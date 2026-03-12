package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface GetMyWorkspaceInvitationsUseCase {

  Mono<PageResult<Invitation>> getMyWorkspaceInvitations(
      GetMyWorkspaceInvitationsQuery query);

}

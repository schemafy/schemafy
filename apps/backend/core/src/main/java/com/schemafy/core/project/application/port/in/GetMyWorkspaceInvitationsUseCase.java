package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;

import reactor.core.publisher.Mono;

public interface GetMyWorkspaceInvitationsUseCase {

  Mono<PageResult<InvitationSummary>> getMyWorkspaceInvitations(
      GetMyWorkspaceInvitationsQuery query);

}

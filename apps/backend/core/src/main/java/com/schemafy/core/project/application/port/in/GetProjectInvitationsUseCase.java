package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;

import reactor.core.publisher.Mono;

public interface GetProjectInvitationsUseCase {

  Mono<PageResult<InvitationSummary>> getProjectInvitations(
      GetProjectInvitationsQuery query);

}

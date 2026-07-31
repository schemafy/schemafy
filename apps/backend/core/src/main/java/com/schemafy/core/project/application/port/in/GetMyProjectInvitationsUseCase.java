package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;

import reactor.core.publisher.Mono;

public interface GetMyProjectInvitationsUseCase {

  Mono<PageResult<InvitationSummary>> getMyProjectInvitations(
      GetMyProjectInvitationsQuery query);

}

package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.CursorResult;

import reactor.core.publisher.Mono;

public interface GetMyInvitationsUseCase {

  Mono<CursorResult<InvitationSummary>> getMyInvitations(
      GetMyInvitationsQuery query);

}

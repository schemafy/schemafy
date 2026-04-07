package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.CursorResult;
import com.schemafy.core.project.domain.Invitation;

import reactor.core.publisher.Mono;

public interface GetMyInvitationsUseCase {

  Mono<CursorResult<Invitation>> getMyInvitations(
      GetMyInvitationsQuery query);

}

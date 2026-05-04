package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.CursorResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.GetMyInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyInvitationsUseCase;
import com.schemafy.core.project.application.port.in.InvitationSummary;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMyInvitationsService implements GetMyInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<CursorResult<InvitationSummary>> getMyInvitations(
      GetMyInvitationsQuery query) {
    return findUserByIdPort.findUserById(query.requesterId())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> invitationPort.findMyPendingInvitationSummaries(
            user.email(), query.cursorId(), query.size() + 1)
            .collectList()
            .map(invitations -> CursorResult.fromFetchedPage(invitations,
                query.size(), InvitationSummary::id)));
  }

}

package com.schemafy.core.project.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.CursorResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.GetMyInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyInvitationsUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMyInvitationsService implements GetMyInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<CursorResult<Invitation>> getMyInvitations(
      GetMyInvitationsQuery query) {
    return findUserByIdPort.findUserById(query.requesterId())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> findInvitations(user.email(), query.cursorId(),
            query.size() + 1)
            .collectList()
            .map(invitations -> toCursorResult(invitations, query.size())));
  }

  private Flux<Invitation> findInvitations(
      String email,
      String cursorId,
      int limit) {
    if (cursorId == null) {
      return invitationPort.findMyInvitationsByEmailAndStatus(email,
          InvitationStatus.PENDING.name(), limit);
    }
    return invitationPort.findMyInvitationsByEmailAndStatusBeforeId(email,
        InvitationStatus.PENDING.name(), cursorId, limit);
  }

  private CursorResult<Invitation> toCursorResult(
      List<Invitation> invitations,
      int size) {
    boolean hasNext = invitations.size() > size;
    List<Invitation> content = hasNext ? invitations.subList(0, size)
        : invitations;
    String nextCursorId = hasNext && !content.isEmpty()
        ? content.getLast().getId()
        : null;
    return CursorResult.of(content, size, hasNext, nextCursorId);
  }

}

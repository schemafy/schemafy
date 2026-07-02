package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.in.InvitationSummary;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMyProjectInvitationsService
    implements GetMyProjectInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<PageResult<InvitationSummary>> getMyProjectInvitations(
      GetMyProjectInvitationsQuery query) {
    return findUserByIdPort.findUserById(query.requesterId())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> invitationPort.countByEmailAndTypeAndStatus(
            user.email(),
            InvitationType.PROJECT.name(),
            InvitationStatus.PENDING.name())
            .flatMap(totalElements -> invitationPort
                .findInvitationSummariesByEmailAndTypeAndStatus(
                    user.email(),
                    InvitationType.PROJECT.name(),
                    InvitationStatus.PENDING.name(),
                    query.size(),
                    query.page() * query.size())
                .collectList()
                .map(invitations -> PageResult.of(invitations, query.page(),
                    query.size(), totalElements))));
  }

}

package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.GetMyWorkspaceInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetMyWorkspaceInvitationsUseCase;
import com.schemafy.domain.project.application.port.out.InvitationPort;
import com.schemafy.domain.project.domain.Invitation;
import com.schemafy.domain.project.domain.InvitationStatus;
import com.schemafy.domain.project.domain.InvitationType;
import com.schemafy.domain.user.application.port.out.FindUserByIdPort;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMyWorkspaceInvitationsService
    implements GetMyWorkspaceInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<PageResult<Invitation>> getMyWorkspaceInvitations(
      GetMyWorkspaceInvitationsQuery query) {
    return findUserByIdPort.findUserById(query.requesterId())
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> invitationPort.countByEmailAndTypeAndStatus(
            user.email(),
            InvitationType.WORKSPACE.name(),
            InvitationStatus.PENDING.name())
            .flatMap(totalElements -> invitationPort
                .findByEmailAndTypeAndStatus(
                    user.email(),
                    InvitationType.WORKSPACE.name(),
                    InvitationStatus.PENDING.name(),
                    query.size(),
                    query.page() * query.size())
                .collectList()
                .map(invitations -> PageResult.of(invitations, query.page(),
                    query.size(), totalElements))));
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.GetWorkspaceInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceInvitationsUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspaceInvitationsService implements GetWorkspaceInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;

  @Override
  public Mono<PageResult<Invitation>> getWorkspaceInvitations(
      GetWorkspaceInvitationsQuery query) {
    String targetType = InvitationType.WORKSPACE.name();

    return workspaceInvitationHelper.validateAdmin(query.workspaceId(),
        query.requesterId())
        .then(invitationPort.countByTarget(targetType, query.workspaceId()))
        .flatMap(totalElements -> invitationPort
            .findInvitationsByTargetAndId(targetType, query.workspaceId(),
                query.size(), query.page() * query.size())
            .collectList()
            .map(invitations -> PageResult.of(invitations, query.page(),
                query.size(), totalElements)));
  }

}

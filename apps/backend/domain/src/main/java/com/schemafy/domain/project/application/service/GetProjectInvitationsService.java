package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.application.port.in.GetProjectInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetProjectInvitationsUseCase;
import com.schemafy.domain.project.application.port.out.InvitationPort;
import com.schemafy.domain.project.domain.Invitation;
import com.schemafy.domain.project.domain.InvitationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectInvitationsService implements GetProjectInvitationsUseCase {

  private final InvitationPort invitationPort;
  private final ProjectInvitationHelper projectInvitationHelper;

  @Override
  public Mono<PageResult<Invitation>> getProjectInvitations(
      GetProjectInvitationsQuery query) {
    String targetType = InvitationType.PROJECT.name();

    return projectInvitationHelper.validateProjectAdmin(query.projectId(),
        query.requesterId())
        .then(invitationPort.countByTarget(targetType, query.projectId()))
        .flatMap(totalElements -> invitationPort
            .findInvitationsByTargetAndId(targetType, query.projectId(),
                query.size(), query.page() * query.size())
            .collectList()
            .map(invitations -> PageResult.of(invitations, query.page(),
                query.size(), totalElements)));
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectInvitationsService implements GetProjectInvitationsUseCase {

  private final InvitationPort invitationPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<PageResult<Invitation>> getProjectInvitations(
      GetProjectInvitationsQuery query) {
    String targetType = InvitationType.PROJECT.name();

    return invitationPort.countByTarget(targetType, query.projectId())
        .flatMap(totalElements -> invitationPort
            .findInvitationsByTargetAndId(targetType, query.projectId(),
                query.size(), query.page() * query.size())
            .collectList()
            .map(invitations -> PageResult.of(invitations, query.page(),
                query.size(), totalElements)));
  }

}

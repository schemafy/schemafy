package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspaceMembersService implements GetWorkspaceMembersUseCase {

  private final WorkspaceMemberPort workspaceMemberPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  public Mono<PageResult<WorkspaceMember>> getWorkspaceMembers(
      GetWorkspaceMembersQuery query) {
    return workspaceAccessHelper.validateMemberAccess(query.workspaceId(),
        query.requesterId())
        .then(workspaceMemberPort.countByWorkspaceIdAndNotDeleted(
            query.workspaceId()))
        .flatMap(totalElements -> workspaceMemberPort
            .findByWorkspaceIdAndNotDeleted(query.workspaceId(), query.size(),
                query.page() * query.size())
            .collectList()
            .map(members -> PageResult.of(members, query.page(), query.size(),
                totalElements)));
  }

}

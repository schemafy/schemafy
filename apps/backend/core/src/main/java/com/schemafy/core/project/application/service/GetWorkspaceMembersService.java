package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspaceMembersService implements GetWorkspaceMembersUseCase {

  private final WorkspaceMemberPort workspaceMemberPort;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.MEMBER)
  public Mono<PageResult<WorkspaceMember>> getWorkspaceMembers(
      GetWorkspaceMembersQuery query) {
    return workspaceMemberPort.countByWorkspaceIdAndNotDeleted(
        query.workspaceId())
        .flatMap(totalElements -> workspaceMemberPort
            .findByWorkspaceIdAndNotDeleted(query.workspaceId(), query.size(),
                query.page() * query.size())
            .collectList()
            .map(members -> PageResult.of(members, query.page(), query.size(),
                totalElements)));
  }

}

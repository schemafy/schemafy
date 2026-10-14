package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.out.ProjectSearchPort;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SearchWorkspaceMembersService implements SearchWorkspaceMembersUseCase {

  private final ProjectSearchPort projectSearchPort;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.MEMBER)
  public Mono<PageResult<MemberSearchResult>> searchWorkspaceMembers(
      SearchWorkspaceMembersQuery query) {
    return projectSearchPort.searchWorkspaceMembers(
        query.workspaceId(), query.search(), query.page(), query.size());
  }

}

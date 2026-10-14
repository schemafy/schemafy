package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsUseCase;
import com.schemafy.core.project.application.port.out.ProjectSearchPort;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SearchWorkspaceProjectsService implements SearchWorkspaceProjectsUseCase {

  private final ProjectSearchPort projectSearchPort;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.MEMBER)
  public Mono<PageResult<ProjectSearchResult>> searchWorkspaceProjects(
      SearchWorkspaceProjectsQuery query) {
    return projectSearchPort.searchWorkspaceProjects(
        query.workspaceId(), query.requesterId(), query.search(),
        query.page(), query.size());
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.SearchProjectMembersQuery;
import com.schemafy.core.project.application.port.in.SearchProjectMembersUseCase;
import com.schemafy.core.project.application.port.out.ProjectSearchPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class SearchProjectMembersService implements SearchProjectMembersUseCase {

  private final ProjectSearchPort projectSearchPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.VIEWER)
  public Mono<PageResult<MemberSearchResult>> searchProjectMembers(
      SearchProjectMembersQuery query) {
    return projectSearchPort.searchProjectMembers(
        query.projectId(), query.search(), query.page(), query.size());
  }

}

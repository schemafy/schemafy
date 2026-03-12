package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.core.project.application.port.in.GetProjectMembersUseCase;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectMembersService implements GetProjectMembersUseCase {

  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<PageResult<ProjectMember>> getProjectMembers(
      GetProjectMembersQuery query) {
    return projectAccessHelper.validateProjectMember(query.projectId(),
        query.requesterId())
        .then(projectMemberPort.countByProjectIdAndNotDeleted(query.projectId()))
        .flatMap(totalElements -> projectMemberPort
            .findByProjectIdAndNotDeleted(query.projectId(), query.size(),
                query.page() * query.size())
            .collectList()
            .map(members -> PageResult.of(members, query.page(), query.size(),
                totalElements)));
  }

}

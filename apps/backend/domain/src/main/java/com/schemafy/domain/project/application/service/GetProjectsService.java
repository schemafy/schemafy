package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.application.port.in.GetProjectsQuery;
import com.schemafy.domain.project.application.port.in.GetProjectsUseCase;
import com.schemafy.domain.project.application.port.in.ProjectSummary;
import com.schemafy.domain.project.application.port.out.ProjectMemberPort;
import com.schemafy.domain.project.application.port.out.ProjectPort;
import com.schemafy.domain.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectsService implements GetProjectsUseCase {

  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<PageResult<ProjectSummary>> getProjects(GetProjectsQuery query) {
    return projectAccessHelper.validateWorkspaceMember(query.workspaceId(),
        query.requesterId())
        .then(Mono.defer(() -> {
          int offset = query.page() * query.size();
          return projectMemberPort.countByWorkspaceIdAndUserId(
              query.workspaceId(),
              query.requesterId())
              .flatMap(totalElements -> Mono.zip(
                  projectPort.findByWorkspaceIdAndUserIdWithPaging(
                      query.workspaceId(), query.requesterId(), query.size(),
                      offset)
                      .collectList(),
                  projectMemberPort.findRolesByWorkspaceIdAndUserIdWithPaging(
                      query.workspaceId(), query.requesterId(), query.size(),
                      offset)
                      .collectList())
                  .flatMap(tuple -> Flux.range(0, tuple.getT1().size())
                      .map(index -> new ProjectSummary(
                          tuple.getT1().get(index),
                          ProjectRole.fromString(tuple.getT2().get(index))))
                      .collectList()
                      .map(content -> PageResult.of(content, query.page(),
                          query.size(), totalElements))));
        }));
  }

}

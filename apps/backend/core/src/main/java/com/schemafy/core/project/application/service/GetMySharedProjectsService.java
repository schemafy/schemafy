package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsQuery;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.ProjectSummary;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMySharedProjectsService implements GetMySharedProjectsUseCase {

  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;

  @Override
  public Mono<PageResult<ProjectSummary>> getMySharedProjects(
      GetMySharedProjectsQuery query) {
    int offset = query.page() * query.size();
    return projectMemberPort.countSharedByUserId(query.requesterId())
        .flatMap(totalElements -> Mono.zip(
            projectPort.findSharedByUserIdWithPaging(query.requesterId(),
                query.size(), offset)
                .collectList(),
            projectMemberPort.findSharedRolesByUserIdWithPaging(
                query.requesterId(), query.size(), offset)
                .collectList())
            .flatMap(tuple -> Flux.range(0, tuple.getT1().size())
                .map(index -> new ProjectSummary(
                    tuple.getT1().get(index),
                    ProjectRole.fromString(tuple.getT2().get(index))))
                .collectList()
                .map(content -> PageResult.of(content, query.page(),
                    query.size(), totalElements))));
  }

}

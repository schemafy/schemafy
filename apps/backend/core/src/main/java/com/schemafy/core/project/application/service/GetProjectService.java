package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.port.in.GetProjectQuery;
import com.schemafy.core.project.application.port.in.GetProjectUseCase;
import com.schemafy.core.project.application.port.in.ProjectDetail;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectService implements GetProjectUseCase {

  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<ProjectDetail> getProject(GetProjectQuery query) {
    return projectAccessHelper.validateProjectMember(query.projectId(),
        query.requesterId())
        .then(projectAccessHelper.findProjectById(query.projectId()))
        .flatMap(project -> projectAccessHelper.buildProjectDetail(project,
            query.requesterId()));
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.port.out.ProjectPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectService implements UpdateProjectUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectPort projectPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<ProjectDetail> updateProject(UpdateProjectCommand command) {
    return projectAccessHelper.validateProjectAdmin(command.projectId(),
        command.requesterId())
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(project -> {
          project.update(command.name(), command.description());
          return projectPort.save(project);
        })
        .flatMap(savedProject -> projectAccessHelper.buildProjectDetail(
            savedProject, command.requesterId()))
        .as(transactionalOperator::transactional);
  }

}

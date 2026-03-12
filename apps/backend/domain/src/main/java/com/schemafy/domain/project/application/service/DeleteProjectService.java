package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.DeleteProjectCommand;
import com.schemafy.domain.project.application.port.in.DeleteProjectUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteProjectService implements DeleteProjectUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;

  @Override
  public Mono<Void> deleteProject(DeleteProjectCommand command) {
    return projectAccessHelper.validateProjectAdmin(command.projectId(),
        command.requesterId())
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(projectCascadeHelper::softDeleteProjectCascade)
        .as(transactionalOperator::transactional);
  }

}

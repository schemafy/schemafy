package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteProjectService implements DeleteProjectUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<Void> deleteProject(DeleteProjectCommand command) {
    return projectAccessHelper.findProjectById(command.projectId())
        .flatMap(projectCascadeHelper::softDeleteProjectCascade)
        .as(transactionalOperator::transactional);
  }

}

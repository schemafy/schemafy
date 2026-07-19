package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteProjectService implements DeleteProjectUseCase {

  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;
  private final ProjectMutationGuard projectMutationGuard;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<Void> deleteProject(DeleteProjectCommand command) {
    return projectMutationGuard.protectWorkspaceAndProjectMutation(command.projectId(),
        () -> projectAccessHelper.findProjectById(command.projectId())
            .flatMap(projectCascadeHelper::softDeleteLockedProjectCascade));
  }

}

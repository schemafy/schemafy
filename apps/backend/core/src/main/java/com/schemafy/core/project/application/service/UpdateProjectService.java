package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectService implements UpdateProjectUseCase {

  private final ProjectPort projectPort;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectMutationGuard projectMutationGuard;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ProjectDetail> updateProject(UpdateProjectCommand command) {
    return projectMutationGuard.protectProjectMutation(command.projectId(),
        () -> projectAccessHelper.findProjectById(command.projectId())
            .flatMap(project -> {
              project.update(command.name(), command.description());
              return projectPort.save(project);
            })
            .flatMap(savedProject -> projectAccessHelper.buildProjectDetail(
                savedProject, command.requesterId())));
  }

}

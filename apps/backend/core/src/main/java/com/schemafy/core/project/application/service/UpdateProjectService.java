package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectService implements UpdateProjectUseCase {

  private final ProjectPort projectPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ProjectDetail> updateProject(UpdateProjectCommand command) {
    return projectPort.updateIfActive(command.projectId(), command.name(),
        command.description())
        .filter(updatedRows -> updatedRows > 0)
        .switchIfEmpty(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND)))
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(updatedProject -> projectAccessHelper.buildProjectDetail(
            updatedProject, command.requesterId()))
        .onErrorMap(DomainException.hasErrorCode(ProjectErrorCode.MEMBER_NOT_FOUND),
            error -> new DomainException(ProjectErrorCode.NOT_FOUND));
  }

}

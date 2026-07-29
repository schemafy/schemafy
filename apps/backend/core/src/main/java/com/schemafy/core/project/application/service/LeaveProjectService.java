package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class LeaveProjectService implements LeaveProjectUseCase {

  private final ProjectAccessHelper projectAccessHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.VIEWER)
  public Mono<Void> leaveProject(LeaveProjectCommand command) {
    return applyLeave(command);
  }

  private Mono<Void> applyLeave(LeaveProjectCommand command) {
    return projectAccessHelper.findProjectMember(command.requesterId(), command.projectId())
        .flatMap(member -> projectAccessHelper.validateWorkspaceAdminGuard(command.projectId(), member)
            .thenReturn(member))
        .flatMap(projectAccessHelper::softDeleteMember);
  }

}

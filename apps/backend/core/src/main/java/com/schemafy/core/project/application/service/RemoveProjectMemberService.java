package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberUseCase;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RemoveProjectMemberService implements RemoveProjectMemberUseCase {

  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectMutationGuard projectMutationGuard;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<Void> removeProjectMember(RemoveProjectMemberCommand command) {
    return projectMutationGuard.protectProjectMutation(command.projectId(),
        () -> projectAccessHelper.findProjectAdminMember(
            command.requesterId(), command.projectId())
            .then(Mono.defer(() -> projectAccessHelper.findProjectMember(
                command.targetUserId(), command.projectId())))
            .flatMap(target -> projectAccessHelper
                .validateWorkspaceAdminGuard(command.projectId(), target)
                .then(projectAccessHelper.softDeleteMember(target))));
  }

}

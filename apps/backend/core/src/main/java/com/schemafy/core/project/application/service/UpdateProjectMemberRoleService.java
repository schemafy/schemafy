package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectMemberRoleService implements UpdateProjectMemberRoleUseCase {

  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectMutationGuard projectMutationGuard;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ProjectMember> updateProjectMemberRole(
      UpdateProjectMemberRoleCommand command) {
    return projectMutationGuard.protectProjectMutation(command.projectId(),
        () -> Mono.zip(
            projectAccessHelper.findProjectAdminMember(
                command.requesterId(), command.projectId()),
            projectAccessHelper.findProjectMember(command.targetUserId(), command.projectId()))
            .flatMap(tuple -> validateAndUpdateMemberRole(
                command, tuple.getT1(), tuple.getT2())));
  }

  private Mono<ProjectMember> validateAndUpdateMemberRole(
      UpdateProjectMemberRoleCommand command,
      ProjectMember requester,
      ProjectMember target) {
    projectAccessHelper.validateRoleChangePermission(requester, target, command.role());
    Mono<Void> workspaceAdminGuard = target.isAdmin() && !command.role().isAdmin()
        ? projectAccessHelper.validateWorkspaceAdminGuard(command.projectId(), target)
        : Mono.empty();

    return workspaceAdminGuard
        .then(projectMemberPort.updateRoleIfActive(target.getProjectId(),
            target.getUserId(), command.role().name()))
        .filter(updatedRows -> updatedRows > 0)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.MEMBER_NOT_FOUND)))
        .then(Mono.fromSupplier(() -> {
          target.updateRole(command.role());
          return target;
        }));
  }

}

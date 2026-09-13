package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceMemberRoleUseCase;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateWorkspaceMemberRoleService
    implements UpdateWorkspaceMemberRoleUseCase {

  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;
  private final WorkspaceMutationGuard workspaceMutationGuard;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<WorkspaceMember> updateWorkspaceMemberRole(
      UpdateWorkspaceMemberRoleCommand command) {
    return workspaceMutationGuard.protectExclusive(command.workspaceId(),
        () -> workspaceAccessHelper
            .findWorkspaceAdminMember(command.requesterId(), command.workspaceId())
            .then(Mono.defer(() -> workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
                command.workspaceId()))
                .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
                    command.workspaceId(),
                    targetMember,
                    member -> member.updateRole(command.role())))
                .flatMap(savedMember -> applyWorkspaceRoleToProjectMemberships(
                    command.workspaceId(),
                    command.targetUserId(),
                    command.role())
                    .thenReturn(savedMember))));
  }

  private Mono<Void> applyWorkspaceRoleToProjectMemberships(
      String workspaceId,
      String userId,
      WorkspaceRole workspaceRole) {
    if (workspaceRole.isAdmin()) {
      return projectMembershipPropagationHelper.syncProjectMembershipsForWorkspaceRole(
          workspaceId,
          userId,
          workspaceRole);
    }

    return projectMembershipPropagationHelper.updateActiveProjectMembershipRoles(
        workspaceId,
        userId,
        workspaceRole);
  }

}

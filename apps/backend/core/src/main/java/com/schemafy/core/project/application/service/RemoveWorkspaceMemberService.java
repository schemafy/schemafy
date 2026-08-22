package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.BaseEntity;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RemoveWorkspaceMemberService implements RemoveWorkspaceMemberUseCase {

  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;
  private final WorkspaceMutationGuard workspaceMutationGuard;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<Void> removeWorkspaceMember(RemoveWorkspaceMemberCommand command) {
    return workspaceMutationGuard.protectExclusive(command.workspaceId(),
        () -> workspaceAccessHelper
            .findWorkspaceAdminMember(command.requesterId(), command.workspaceId())
            .then(Mono.defer(() -> workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
                command.workspaceId())
                .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
                    command.workspaceId(), targetMember, BaseEntity::delete))
                .then(projectMembershipPropagationHelper.removeFromAllProjects(
                    command.workspaceId(), command.targetUserId())))));
  }

}

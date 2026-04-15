package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

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

  private final TransactionalOperator transactionalOperator;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<WorkspaceMember> updateWorkspaceMemberRole(
      UpdateWorkspaceMemberRoleCommand command) {
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 lock보다 먼저 수행
    // commit 시점 권한까지 엄밀히 보장하려면 lock 이후 재검증 필요
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(),
        command.requesterId())
        .then(Mono.defer(() -> updateWorkspaceMemberRoleWithinWriteScope(command)
            .as(transactionalOperator::transactional)));
  }

  private Mono<WorkspaceMember> updateWorkspaceMemberRoleWithinWriteScope(
      UpdateWorkspaceMemberRoleCommand command) {
    return workspaceAccessHelper.requireWorkspaceForWrite(
        command.workspaceId())
        .then(workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
            command.workspaceId()))
        .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
            command.workspaceId(),
            targetMember,
            member -> member.updateRole(command.role())))
        .flatMap(savedMember -> applyWorkspaceRoleToProjectMemberships(
            command.workspaceId(),
            command.targetUserId(),
            command.role())
            .thenReturn(savedMember));
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

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
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 트랜잭션 진입 전 수행
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(),
        command.requesterId())
        .then(Mono.defer(() -> doUpdateWorkspaceMemberRole(command)
            .as(transactionalOperator::transactional)));
  }

  private Mono<WorkspaceMember> doUpdateWorkspaceMemberRole(
      UpdateWorkspaceMemberRoleCommand command) {
    return workspaceAccessHelper.findWorkspaceOrThrow(
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

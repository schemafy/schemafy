package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

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

  private final TransactionalOperator transactionalOperator;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<Void> removeWorkspaceMember(RemoveWorkspaceMemberCommand command) {
    return workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
        command.workspaceId())
        .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
            command.workspaceId(), targetMember, BaseEntity::delete))
        .then(projectMembershipPropagationHelper.removeFromAllProjects(
            command.workspaceId(), command.targetUserId()))
        .as(transactionalOperator::transactional);
  }

}

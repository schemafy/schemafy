package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.domain.project.application.port.in.RemoveWorkspaceMemberUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RemoveWorkspaceMemberService implements RemoveWorkspaceMemberUseCase {

  private final TransactionalOperator transactionalOperator;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<Void> removeWorkspaceMember(RemoveWorkspaceMemberCommand command) {
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(),
        command.requesterId())
        .then(workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
            command.workspaceId()))
        .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
            command.workspaceId(), targetMember, member -> member.delete()))
        .then(projectMembershipPropagationHelper.removeFromAllProjects(
            command.workspaceId(), command.targetUserId()))
        .as(transactionalOperator::transactional);
  }

}

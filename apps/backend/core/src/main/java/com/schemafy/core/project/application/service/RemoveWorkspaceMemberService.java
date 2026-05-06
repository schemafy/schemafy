package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveWorkspaceMemberUseCase;

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
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 트랜잭션 진입 전 수행
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(),
        command.requesterId())
        .then(Mono.defer(() -> doRemoveWorkspaceMember(command)
            .as(transactionalOperator::transactional)));
  }

  private Mono<Void> doRemoveWorkspaceMember(
      RemoveWorkspaceMemberCommand command) {
    return workspaceAccessHelper.findWorkspaceOrThrow(
        command.workspaceId())
        .then(workspaceAccessHelper.findWorkspaceMember(command.targetUserId(),
            command.workspaceId()))
        .flatMap(targetMember -> workspaceAccessHelper.modifyMemberWithAdminGuard(
            command.workspaceId(), targetMember, member -> member.delete()))
        .then(projectMembershipPropagationHelper.removeFromAllProjects(
            command.workspaceId(), command.targetUserId()));
  }

}

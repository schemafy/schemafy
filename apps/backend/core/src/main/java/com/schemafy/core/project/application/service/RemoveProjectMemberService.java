package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RemoveProjectMemberService implements RemoveProjectMemberUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<Void> removeProjectMember(RemoveProjectMemberCommand command) {
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 lock보다 먼저 수행
    // commit 시점 권한까지 엄밀히 보장하려면 lock 이후 재검증 필요
    return projectAccessHelper.validateProjectAdmin(command.projectId(), command.requesterId())
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(project -> Mono.defer(() -> removeProjectMemberWithinWriteScope(
            command, project.getWorkspaceId())
            .as(transactionalOperator::transactional)));
  }

  private Mono<Void> removeProjectMemberWithinWriteScope(
      RemoveProjectMemberCommand command,
      String workspaceId) {
    return projectAccessHelper.requireProjectWithinWorkspaceForWrite(
        workspaceId, command.projectId())
        .then(projectAccessHelper.findProjectMember(command.targetUserId(), command.projectId()))
        .flatMap(target -> projectAccessHelper.validateWorkspaceAdminGuard(
            command.projectId(), target)
            .then(projectAccessHelper.softDeleteMember(target)));
  }

}

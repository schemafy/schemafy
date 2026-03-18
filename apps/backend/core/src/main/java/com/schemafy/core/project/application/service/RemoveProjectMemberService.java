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
    return projectAccessHelper.validateProjectAdmin(command.projectId(), command.requesterId())
        .then(projectAccessHelper.findProjectMember(command.targetUserId(), command.projectId()))
        .flatMap(target -> projectAccessHelper.validateWorkspaceAdminGuard(command.projectId(), target)
            .then(projectAccessHelper.softDeleteMember(target))
            .as(transactionalOperator::transactional));
  }

}

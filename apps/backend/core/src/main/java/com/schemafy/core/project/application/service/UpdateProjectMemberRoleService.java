package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectMemberRoleService implements UpdateProjectMemberRoleUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<ProjectMember> updateProjectMemberRole(
      UpdateProjectMemberRoleCommand command) {
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 lock보다 먼저 수행
    // commit 시점 권한까지 엄밀히 보장하려면 lock 이후 재검증 필요
    return projectAccessHelper.validateProjectAdmin(command.projectId(),
        command.requesterId())
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(project -> Mono.defer(() -> updateProjectMemberRoleWithinWriteScope(
            command, project.getWorkspaceId())
            .as(transactionalOperator::transactional)));
  }

  private Mono<ProjectMember> updateProjectMemberRoleWithinWriteScope(
      UpdateProjectMemberRoleCommand command,
      String workspaceId) {
    return projectAccessHelper.requireProjectWithinWorkspaceForWrite(
        workspaceId, command.projectId())
        .then(projectAccessHelper.findProjectMember(command.requesterId(),
            command.projectId()))
        .flatMap(requester -> projectAccessHelper.findProjectMember(
            command.targetUserId(), command.projectId())
            .flatMap(target -> {

              projectAccessHelper.validateRoleChangePermission(requester, target, command.role());
              Mono<Void> workspaceAdminGuard = target.isAdmin() && !command.role().isAdmin()
                  ? projectAccessHelper.validateWorkspaceAdminGuard(
                      command.projectId(), target)
                  : Mono.empty();

              return workspaceAdminGuard
                  .then(Mono.fromSupplier(() -> {
                    target.updateRole(command.role());
                    return target;
                  }))
                  .flatMap(projectMemberPort::save);
            }));
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleUseCase;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectMemberRoleService implements UpdateProjectMemberRoleUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ProjectMember> updateProjectMemberRole(
      UpdateProjectMemberRoleCommand command) {
    return Mono.zip(
        projectAccessHelper.findProjectMember(command.requesterId(),
            command.projectId()),
        projectAccessHelper.findProjectMember(command.targetUserId(),
            command.projectId()))
        .flatMap(tuple -> {
          ProjectMember requester = tuple.getT1();
          ProjectMember target = tuple.getT2();

          projectAccessHelper.validateRoleChangePermission(requester, target, command.role());
          Mono<Void> workspaceAdminGuard = target.isAdmin() && !command.role().isAdmin()
              ? projectAccessHelper.validateWorkspaceAdminGuard(command.projectId(), target)
              : Mono.empty();

          return workspaceAdminGuard
              .then(Mono.fromSupplier(() -> {
                target.updateRole(command.role());
                return target;
              }))
              .flatMap(projectMemberPort::save);
        })
        .as(transactionalOperator::transactional);
  }

}

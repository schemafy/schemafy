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
    return projectAccessHelper.validateProjectAdmin(command.projectId(),
        command.requesterId())
        .then(Mono.zip(
            projectAccessHelper.findProjectMember(command.requesterId(),
                command.projectId()),
            projectAccessHelper.findProjectMember(command.targetUserId(),
                command.projectId())))
        .flatMap(tuple -> {
          ProjectMember requester = tuple.getT1();
          ProjectMember target = tuple.getT2();

          projectAccessHelper.validateRoleChangePermission(requester, target,
              command.role());
          target.updateRole(command.role());
          return projectMemberPort.save(target);
        })
        .as(transactionalOperator::transactional);
  }

}

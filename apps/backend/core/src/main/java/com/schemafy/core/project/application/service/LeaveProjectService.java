package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class LeaveProjectService implements LeaveProjectUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectPort projectPort;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;

  @Override
  public Mono<Void> leaveProject(LeaveProjectCommand command) {
    return projectPort.findById(command.projectId())
        .flatMap(project -> {
          if (project.isDeleted()) {
            return softDeleteProjectMemberFallback(command);
          }
          return Mono.defer(() -> leaveProjectWithinWriteScope(
              command, project.getWorkspaceId())
              .thenReturn(Boolean.TRUE)
              .as(transactionalOperator::transactional))
              .switchIfEmpty(Mono.defer(() -> softDeleteProjectMemberFallback(command)));
        })
        .switchIfEmpty(Mono.defer(() -> softDeleteProjectMemberFallback(command)))
        .then();
  }

  private Mono<Void> leaveProjectWithinWriteScope(
      LeaveProjectCommand command,
      String workspaceId) {
    return projectAccessHelper.lockProjectWithinWorkspaceForWrite(
        workspaceId, command.projectId())
        .flatMap(project -> projectAccessHelper
            .findProjectMember(command.requesterId(), command.projectId())
            .flatMap(member -> projectAccessHelper
                .validateWorkspaceAdminGuard(command.projectId(), member)
                .thenReturn(member))
            .flatMap(member -> projectMemberPort
                .countByProjectIdAndNotDeleted(command.projectId())
                .flatMap(memberCount -> {
                  if (memberCount <= 1) {
                    return projectCascadeHelper.softDeleteProjectCascade(project);
                  }
                  return projectAccessHelper.softDeleteMember(member);
                })));
  }

  private Mono<Boolean> softDeleteProjectMemberFallback(
      LeaveProjectCommand command) {
    return Mono.defer(() -> projectAccessHelper
        .findProjectMember(command.requesterId(), command.projectId())
        .flatMap(member -> projectAccessHelper.softDeleteMember(member)
            .thenReturn(Boolean.TRUE))
        .as(transactionalOperator::transactional));
  }

}

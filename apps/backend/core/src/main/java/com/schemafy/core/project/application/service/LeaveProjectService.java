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
  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final ProjectAccessHelper projectAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;

  @Override
  public Mono<Void> leaveProject(LeaveProjectCommand command) {
    return projectAccessHelper.findProjectMember(command.requesterId(),
        command.projectId())
        .flatMap(member -> projectMemberPort.countByProjectIdAndNotDeleted(
            command.projectId())
            .flatMap(memberCount -> {
              if (memberCount <= 1) {
                return projectPort.findById(command.projectId())
                    .flatMap(projectCascadeHelper::softDeleteProjectCascade)
                    .switchIfEmpty(projectAccessHelper.softDeleteMember(member))
                    .then();
              }
              return projectAccessHelper.softDeleteMember(member);
            }))
        .as(transactionalOperator::transactional);
  }

}

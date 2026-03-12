package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.LeaveWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.LeaveWorkspaceUseCase;
import com.schemafy.domain.project.application.port.out.InvitationPort;
import com.schemafy.domain.project.application.port.out.ProjectPort;
import com.schemafy.domain.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.domain.project.application.port.out.WorkspacePort;
import com.schemafy.domain.project.domain.InvitationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class LeaveWorkspaceService implements LeaveWorkspaceUseCase {

  private final TransactionalOperator transactionalOperator;
  private final WorkspacePort workspacePort;
  private final ProjectPort projectPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final InvitationPort invitationPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<Void> leaveWorkspace(LeaveWorkspaceCommand command) {
    return workspaceAccessHelper.findWorkspaceMember(command.requesterId(),
        command.workspaceId())
        .flatMap(member -> workspaceMemberPort
            .countByWorkspaceIdAndNotDeleted(command.workspaceId())
            .flatMap(totalMembers -> {
              if (totalMembers == 1) {
                return doDeleteWorkspace(command.workspaceId());
              }

              return workspaceAccessHelper.modifyMemberWithAdminGuard(
                  command.workspaceId(), member, workspaceMember -> {
                    workspaceMember.delete();
                  })
                  .then(projectMembershipPropagationHelper.removeFromAllProjects(
                      command.workspaceId(),
                      command.requesterId()));
            }))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> doDeleteWorkspace(String workspaceId) {
    return workspaceAccessHelper.findWorkspaceOrThrow(workspaceId)
        .flatMap(workspace -> {
          workspace.delete();
          return workspacePort.save(workspace)
              .then(softDeleteWorkspaceCascade(workspaceId));
        });
  }

  private Mono<Void> softDeleteWorkspaceCascade(String workspaceId) {
    return projectPort.findByWorkspaceId(workspaceId)
        .concatMap(projectCascadeHelper::softDeleteProjectCascade)
        .then(workspaceMemberPort.softDeleteByWorkspaceId(workspaceId))
        .then(invitationPort.softDeleteByTarget(
            InvitationType.WORKSPACE.name(),
            workspaceId))
        .then();
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.core.project.application.port.in.DeleteWorkspaceUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.InvitationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteWorkspaceService implements DeleteWorkspaceUseCase {

  private final TransactionalOperator transactionalOperator;
  private final WorkspacePort workspacePort;
  private final ProjectPort projectPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final InvitationPort invitationPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectCascadeHelper projectCascadeHelper;

  @Override
  public Mono<Void> deleteWorkspace(DeleteWorkspaceCommand command) {
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(), command.requesterId())
        .then(Mono.defer(() -> doDeleteWorkspace(command.workspaceId())
            .as(transactionalOperator::transactional)));
  }

  private Mono<Void> doDeleteWorkspace(String workspaceId) {
    return workspaceAccessHelper.requireWorkspaceForWrite(workspaceId)
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
        .then(invitationPort.softDeleteByTarget(InvitationType.WORKSPACE.name(), workspaceId))
        .then();
  }

}

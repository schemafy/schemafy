package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.UpdateWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.WorkspaceDetail;
import com.schemafy.domain.project.application.port.out.WorkspacePort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateWorkspaceService implements UpdateWorkspaceUseCase {

  private final TransactionalOperator transactionalOperator;
  private final WorkspacePort workspacePort;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  public Mono<WorkspaceDetail> updateWorkspace(UpdateWorkspaceCommand command) {
    return workspaceAccessHelper.validateAdminAccess(command.workspaceId(),
        command.requesterId())
        .then(workspaceAccessHelper.findWorkspaceOrThrow(command.workspaceId()))
        .flatMap(workspace -> {
          workspace.update(command.name(), command.description());
          return workspacePort.save(workspace);
        })
        .flatMap(savedWorkspace -> workspaceAccessHelper.buildWorkspaceDetail(
            savedWorkspace, command.requesterId()))
        .as(transactionalOperator::transactional);
  }

}

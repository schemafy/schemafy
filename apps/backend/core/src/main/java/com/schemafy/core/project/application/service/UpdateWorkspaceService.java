package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.core.project.application.port.in.UpdateWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateWorkspaceService implements UpdateWorkspaceUseCase {

  private final WorkspacePort workspacePort;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<WorkspaceDetail> updateWorkspace(UpdateWorkspaceCommand command) {
    return workspacePort.updateIfActive(command.workspaceId(), command.name(),
        command.description())
        .flatMap(updatedRows -> updatedRows > 0
            ? workspaceAccessHelper.findWorkspaceOrThrow(command.workspaceId())
            : Mono.error(new DomainException(WorkspaceErrorCode.NOT_FOUND)))
        .flatMap(updatedWorkspace -> workspaceAccessHelper.buildWorkspaceDetail(
            updatedWorkspace, command.requesterId()));
  }

}

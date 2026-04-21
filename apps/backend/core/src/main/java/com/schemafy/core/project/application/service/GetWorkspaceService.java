package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.core.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.domain.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspaceService implements GetWorkspaceUseCase {

  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.MEMBER)
  public Mono<WorkspaceDetail> getWorkspace(GetWorkspaceQuery query) {
    return workspaceAccessHelper.findWorkspaceOrThrow(query.workspaceId())
        .flatMap(workspace -> workspaceAccessHelper.buildWorkspaceDetail(
            workspace, query.requesterId()));
  }

}

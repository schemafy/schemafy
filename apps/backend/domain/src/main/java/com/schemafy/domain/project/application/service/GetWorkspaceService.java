package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.domain.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.WorkspaceDetail;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspaceService implements GetWorkspaceUseCase {

  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  public Mono<WorkspaceDetail> getWorkspace(GetWorkspaceQuery query) {
    return workspaceAccessHelper.validateMemberAccess(query.workspaceId(),
        query.requesterId())
        .then(workspaceAccessHelper.findWorkspaceOrThrow(query.workspaceId()))
        .flatMap(workspace -> workspaceAccessHelper.buildWorkspaceDetail(
            workspace, query.requesterId()));
  }

}

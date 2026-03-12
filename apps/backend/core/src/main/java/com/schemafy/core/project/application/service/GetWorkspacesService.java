package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.GetWorkspacesQuery;
import com.schemafy.core.project.application.port.in.GetWorkspacesUseCase;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Workspace;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetWorkspacesService implements GetWorkspacesUseCase {

  private final WorkspacePort workspacePort;

  @Override
  public Mono<PageResult<Workspace>> getWorkspaces(GetWorkspacesQuery query) {
    return workspacePort.countByUserId(query.requesterId())
        .flatMap(sizeOfWorkspace -> workspacePort
            .findByUserIdWithPaging(query.requesterId(), query.size(),
                query.page() * query.size())
            .collectList()
            .map(content -> PageResult.of(content, query.page(), query.size(),
                sizeOfWorkspace)));
  }

}

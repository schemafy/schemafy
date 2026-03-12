package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface GetWorkspaceMembersUseCase {

  Mono<PageResult<WorkspaceMember>> getWorkspaceMembers(
      GetWorkspaceMembersQuery query);

}

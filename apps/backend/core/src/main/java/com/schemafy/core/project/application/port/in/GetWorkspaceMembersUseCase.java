package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface GetWorkspaceMembersUseCase {

  Mono<PageResult<WorkspaceMember>> getWorkspaceMembers(
      GetWorkspaceMembersQuery query);

}

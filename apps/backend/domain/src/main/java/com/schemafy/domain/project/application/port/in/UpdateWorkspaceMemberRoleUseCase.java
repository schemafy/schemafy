package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface UpdateWorkspaceMemberRoleUseCase {

  Mono<WorkspaceMember> updateWorkspaceMemberRole(
      UpdateWorkspaceMemberRoleCommand command);

}

package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface AddWorkspaceMemberUseCase {

  Mono<WorkspaceMember> addWorkspaceMember(AddWorkspaceMemberCommand command);

}

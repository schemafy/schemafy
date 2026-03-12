package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.WorkspaceMember;

import reactor.core.publisher.Mono;

public interface AddWorkspaceMemberUseCase {

  Mono<WorkspaceMember> addWorkspaceMember(AddWorkspaceMemberCommand command);

}

package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Mono;

public interface UpdateProjectMemberRoleUseCase {

  Mono<ProjectMember> updateProjectMemberRole(
      UpdateProjectMemberRoleCommand command);

}

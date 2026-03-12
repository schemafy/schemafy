package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.ProjectMember;

import reactor.core.publisher.Mono;

public interface UpdateProjectMemberRoleUseCase {

  Mono<ProjectMember> updateProjectMemberRole(
      UpdateProjectMemberRoleCommand command);

}

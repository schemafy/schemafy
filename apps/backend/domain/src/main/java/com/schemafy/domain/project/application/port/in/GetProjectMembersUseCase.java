package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.domain.ProjectMember;

import reactor.core.publisher.Mono;

public interface GetProjectMembersUseCase {

  Mono<PageResult<ProjectMember>> getProjectMembers(GetProjectMembersQuery query);

}

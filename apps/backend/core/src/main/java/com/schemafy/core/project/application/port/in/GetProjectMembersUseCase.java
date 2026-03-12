package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Mono;

public interface GetProjectMembersUseCase {

  Mono<PageResult<ProjectMember>> getProjectMembers(GetProjectMembersQuery query);

}

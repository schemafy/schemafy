package com.schemafy.core.project.application.port.in;

import com.schemafy.core.common.PageResult;

import reactor.core.publisher.Mono;

public interface SearchProjectMembersUseCase {

  Mono<PageResult<MemberSearchResult>> searchProjectMembers(
      SearchProjectMembersQuery query);

}

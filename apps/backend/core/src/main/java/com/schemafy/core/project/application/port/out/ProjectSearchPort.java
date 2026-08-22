package com.schemafy.core.project.application.port.out;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;

import reactor.core.publisher.Mono;

public interface ProjectSearchPort {

  Mono<PageResult<ProjectSearchResult>> searchWorkspaceProjects(
      String workspaceId,
      String requesterId,
      String search,
      int page,
      int size);

  Mono<PageResult<ProjectSearchResult>> searchSharedProjects(
      String requesterId,
      String search,
      int page,
      int size);

  Mono<PageResult<MemberSearchResult>> searchWorkspaceMembers(
      String workspaceId,
      String search,
      int page,
      int size);

  Mono<PageResult<MemberSearchResult>> searchProjectMembers(
      String projectId,
      String search,
      int page,
      int size);

}

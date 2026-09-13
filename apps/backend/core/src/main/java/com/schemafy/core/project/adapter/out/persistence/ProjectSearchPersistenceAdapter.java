package com.schemafy.core.project.adapter.out.persistence;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.common.PersistenceAdapter;
import com.schemafy.core.common.persistence.SqlLikePattern;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.application.port.out.ProjectSearchPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
public class ProjectSearchPersistenceAdapter implements ProjectSearchPort {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Override
  public Mono<PageResult<ProjectSearchResult>> searchWorkspaceProjects(
      String workspaceId,
      String requesterId,
      String search,
      int page,
      int size) {
    String pattern = SqlLikePattern.contains(search);
    return toPageResult(
        projectRepository.countSearchByWorkspaceIdAndUserId(
            workspaceId, requesterId, pattern),
        projectRepository.searchByWorkspaceIdAndUserId(
            workspaceId, requesterId, pattern, size, page * size),
        page, size);
  }

  @Override
  public Mono<PageResult<ProjectSearchResult>> searchSharedProjects(
      String requesterId,
      String search,
      int page,
      int size) {
    String pattern = SqlLikePattern.contains(search);
    return toPageResult(
        projectRepository.countSearchSharedByUserId(requesterId, pattern),
        projectRepository.searchSharedByUserId(
            requesterId, pattern, size, page * size),
        page, size);
  }

  @Override
  public Mono<PageResult<MemberSearchResult>> searchWorkspaceMembers(
      String workspaceId,
      String search,
      int page,
      int size) {
    String pattern = SqlLikePattern.contains(search);
    return toPageResult(
        workspaceMemberRepository.countByWorkspaceIdAndUser(workspaceId, pattern),
        workspaceMemberRepository.searchMemberResultsByWorkspaceIdAndUser(
            workspaceId, pattern, size, page * size),
        page, size);
  }

  @Override
  public Mono<PageResult<MemberSearchResult>> searchProjectMembers(
      String projectId,
      String search,
      int page,
      int size) {
    String pattern = SqlLikePattern.contains(search);
    return toPageResult(
        projectMemberRepository.countByProjectIdAndUser(projectId, pattern),
        projectMemberRepository.searchMemberResultsByProjectIdAndUser(
            projectId, pattern, size, page * size),
        page, size);
  }

  private <T> Mono<PageResult<T>> toPageResult(
      Mono<Long> totalElements,
      Flux<T> content,
      int page,
      int size) {
    return totalElements.flatMap(total -> content.collectList()
        .map(items -> PageResult.of(items, page, size, total)));
  }

}

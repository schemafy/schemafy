package com.schemafy.api.project.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.PageResponse;
import com.schemafy.api.project.controller.dto.request.MemberSearchCategory;
import com.schemafy.api.project.controller.dto.request.MemberSearchRequest;
import com.schemafy.api.project.controller.dto.request.ProjectSearchCategory;
import com.schemafy.api.project.controller.dto.request.ProjectSearchRequest;
import com.schemafy.api.project.controller.dto.response.MemberSearchResponse;
import com.schemafy.api.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.application.port.in.SearchProjectMembersQuery;
import com.schemafy.core.project.application.port.in.SearchProjectMembersUseCase;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchSharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceMembersUseCase;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsQuery;
import com.schemafy.core.project.application.port.in.SearchWorkspaceProjectsUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class SearchController {

  private final SearchWorkspaceProjectsUseCase searchWorkspaceProjectsUseCase;
  private final SearchSharedProjectsUseCase searchSharedProjectsUseCase;
  private final SearchWorkspaceMembersUseCase searchWorkspaceMembersUseCase;
  private final SearchProjectMembersUseCase searchProjectMembersUseCase;

  @GetMapping("/search/projects")
  public Mono<PageResponse<ProjectSummaryResponse>> searchProjects(
      @RequestParam ProjectSearchCategory category,
      @RequestParam(required = false) String workspaceId,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") @PositiveOrZero @Max(10000) int page,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication authentication) {
    ProjectSearchRequest request = new ProjectSearchRequest(
        category, workspaceId, search, page, size);
    String requesterId = authentication.getName();
    Mono<PageResult<ProjectSearchResult>> result = switch (request.category()) {
    case WORKSPACE -> searchWorkspaceProjectsUseCase.searchWorkspaceProjects(
        new SearchWorkspaceProjectsQuery(
            request.workspaceId(), requesterId, request.search(),
            request.page(), request.size()));
    case SHARED -> searchSharedProjectsUseCase.searchSharedProjects(
        new SearchSharedProjectsQuery(
            requesterId, request.search(), request.page(), request.size()));
    };
    return result.map(pageResult -> PageResponse.of(
        pageResult.content().stream()
            .map(ProjectSummaryResponse::from)
            .toList(),
        pageResult.page(),
        pageResult.size(),
        pageResult.totalElements()));
  }

  @GetMapping("/search/members")
  public Mono<PageResponse<MemberSearchResponse>> searchMembers(
      @RequestParam MemberSearchCategory category,
      @RequestParam(required = false) String workspaceId,
      @RequestParam(required = false) String projectId,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") @PositiveOrZero @Max(10000) int page,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication authentication) {
    MemberSearchRequest request = new MemberSearchRequest(
        category, workspaceId, projectId, search, page, size);
    String requesterId = authentication.getName();
    Mono<PageResult<MemberSearchResult>> result = switch (request.category()) {
    case WORKSPACE -> searchWorkspaceMembersUseCase.searchWorkspaceMembers(
        new SearchWorkspaceMembersQuery(
            request.workspaceId(), requesterId, request.search(),
            request.page(), request.size()));
    case PROJECT -> searchProjectMembersUseCase.searchProjectMembers(
        new SearchProjectMembersQuery(
            request.projectId(), requesterId, request.search(),
            request.page(), request.size()));
    };
    return result.map(pageResult -> PageResponse.of(
        pageResult.content().stream()
            .map(MemberSearchResponse::from)
            .toList(),
        pageResult.page(),
        pageResult.size(),
        pageResult.totalElements()));
  }

}

package com.schemafy.api.project.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.PageResponse;
import com.schemafy.api.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.api.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.api.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.api.project.controller.dto.request.UpdateProjectShareLinkRequest;
import com.schemafy.api.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.api.project.controller.dto.response.ProjectResponse;
import com.schemafy.api.project.controller.dto.response.ProjectShareLinkResponse;
import com.schemafy.api.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.api.project.orchestrator.ProjectMemberOrchestrator;
import com.schemafy.core.project.application.port.in.CreateProjectCommand;
import com.schemafy.core.project.application.port.in.CreateProjectUseCase;
import com.schemafy.core.project.application.port.in.DeleteProjectCommand;
import com.schemafy.core.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsQuery;
import com.schemafy.core.project.application.port.in.GetMySharedProjectsUseCase;
import com.schemafy.core.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.core.project.application.port.in.GetProjectQuery;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.in.GetProjectUseCase;
import com.schemafy.core.project.application.port.in.GetProjectsQuery;
import com.schemafy.core.project.application.port.in.GetProjectsUseCase;
import com.schemafy.core.project.application.port.in.LeaveProjectCommand;
import com.schemafy.core.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.core.project.application.port.in.RemoveProjectMemberUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.in.UpdateProjectUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectController {

  private final CreateProjectUseCase createProjectUseCase;
  private final GetProjectsUseCase getProjectsUseCase;
  private final GetMySharedProjectsUseCase getMySharedProjectsUseCase;
  private final GetProjectUseCase getProjectUseCase;
  private final UpdateProjectUseCase updateProjectUseCase;
  private final DeleteProjectUseCase deleteProjectUseCase;
  private final RemoveProjectMemberUseCase removeProjectMemberUseCase;
  private final LeaveProjectUseCase leaveProjectUseCase;
  private final GetProjectShareLinkUseCase getProjectShareLinkUseCase;
  private final UpdateProjectShareLinkUseCase updateProjectShareLinkUseCase;
  private final ProjectMemberOrchestrator projectMemberOrchestrator;

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  @PostMapping("/workspaces/{workspaceId}/projects")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ProjectResponse> createProject(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return createProjectUseCase.createProject(new CreateProjectCommand(
        workspaceId,
        request.dbVendorId(),
        request.name(),
        request.description(),
        userId))
        .map(ProjectResponse::from);
  }

  @GetMapping("/workspaces/{workspaceId}/projects")
  public Mono<PageResponse<ProjectSummaryResponse>> getProjects(
      @PathVariable String workspaceId,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return getProjectsUseCase.getProjects(new GetProjectsQuery(
        workspaceId,
        userId,
        page,
        size))
        .map(result -> PageResponse.of(
            result.content().stream().map(ProjectSummaryResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @GetMapping("/projects/{projectId}")
  public Mono<ProjectResponse> getProject(
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return getProjectUseCase.getProject(new GetProjectQuery(projectId, userId))
        .map(ProjectResponse::from);
  }

  @GetMapping("/projects/shared/me")
  public Mono<PageResponse<ProjectSummaryResponse>> getMySharedProjects(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return getMySharedProjectsUseCase.getMySharedProjects(
        new GetMySharedProjectsQuery(userId, page, size))
        .map(result -> PageResponse.of(
            result.content().stream()
                .map(ProjectSummaryResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @PutMapping("/projects/{projectId}")
  public Mono<ProjectResponse> updateProject(
      @PathVariable String projectId,
      @Valid @RequestBody UpdateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return updateProjectUseCase.updateProject(new UpdateProjectCommand(
        projectId,
        request.name(),
        request.description(),
        userId))
        .map(ProjectResponse::from);
  }

  @DeleteMapping("/projects/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteProject(@PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return deleteProjectUseCase.deleteProject(new DeleteProjectCommand(
        projectId,
        userId));
  }

  @GetMapping("/projects/{projectId}/members")
  public Mono<PageResponse<ProjectMemberResponse>> getMembers(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "5") @Positive @Max(100) int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectMemberOrchestrator.getMembers(new GetProjectMembersQuery(
        projectId,
        userId,
        page,
        size))
        .map(result -> result.map(ProjectMemberResponse::from));
  }

  @PatchMapping("/projects/{projectId}/members/{userId}/role")
  public Mono<ProjectMemberResponse> updateMemberRole(
      @PathVariable String projectId,
      @PathVariable String userId,
      @Valid @RequestBody UpdateProjectMemberRoleRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return projectMemberOrchestrator.updateMemberRole(
        new UpdateProjectMemberRoleCommand(
            projectId,
            userId,
            request.role(),
            requesterId))
        .map(ProjectMemberResponse::from);
  }

  @DeleteMapping("/projects/{projectId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(@PathVariable String projectId, @PathVariable String userId,
      Authentication authentication) {
    String requester = authentication.getName();
    return removeProjectMemberUseCase.removeProjectMember(
        new RemoveProjectMemberCommand(projectId, userId, requester));
  }

  @DeleteMapping("/projects/{projectId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveProject(
      @PathVariable String projectId, Authentication authentication) {
    String userId = authentication.getName();
    return leaveProjectUseCase.leaveProject(new LeaveProjectCommand(
        projectId,
        userId));
  }

  @GetMapping("/projects/{projectId}/share-link")
  public Mono<ProjectShareLinkResponse> getProjectShareLink(
      @PathVariable String version,
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return getProjectShareLinkUseCase.getProjectShareLink(
        new GetProjectShareLinkQuery(projectId, userId))
        .map(link -> ProjectShareLinkResponse.of(link, baseUrl, version))
        .defaultIfEmpty(ProjectShareLinkResponse.inactive());
  }

  @PatchMapping("/projects/{projectId}/share-link")
  public Mono<ProjectShareLinkResponse> updateProjectShareLink(
      @PathVariable String version,
      @PathVariable String projectId,
      @Valid @RequestBody UpdateProjectShareLinkRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return updateProjectShareLinkUseCase.updateProjectShareLink(
        new UpdateProjectShareLinkCommand(
            projectId,
            request.isActive(),
            userId))
        .map(link -> ProjectShareLinkResponse.of(link, baseUrl, version))
        .defaultIfEmpty(ProjectShareLinkResponse.inactive());
  }

}

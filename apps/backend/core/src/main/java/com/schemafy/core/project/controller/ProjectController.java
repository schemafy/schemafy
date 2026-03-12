package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.controller.dto.response.ProjectResponse;
import com.schemafy.core.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.project.controller.dto.response.ShareLinkResponse;
import com.schemafy.core.project.orchestrator.ProjectMemberOrchestrator;
import com.schemafy.domain.project.application.port.in.CreateProjectCommand;
import com.schemafy.domain.project.application.port.in.CreateProjectUseCase;
import com.schemafy.domain.project.application.port.in.CreateShareLinkCommand;
import com.schemafy.domain.project.application.port.in.CreateShareLinkUseCase;
import com.schemafy.domain.project.application.port.in.DeleteProjectCommand;
import com.schemafy.domain.project.application.port.in.DeleteProjectUseCase;
import com.schemafy.domain.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.domain.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.domain.project.application.port.in.GetProjectMembersQuery;
import com.schemafy.domain.project.application.port.in.GetProjectQuery;
import com.schemafy.domain.project.application.port.in.GetProjectUseCase;
import com.schemafy.domain.project.application.port.in.GetProjectsQuery;
import com.schemafy.domain.project.application.port.in.GetProjectsUseCase;
import com.schemafy.domain.project.application.port.in.GetShareLinkQuery;
import com.schemafy.domain.project.application.port.in.GetShareLinkUseCase;
import com.schemafy.domain.project.application.port.in.GetShareLinksQuery;
import com.schemafy.domain.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.domain.project.application.port.in.LeaveProjectCommand;
import com.schemafy.domain.project.application.port.in.LeaveProjectUseCase;
import com.schemafy.domain.project.application.port.in.RemoveProjectMemberCommand;
import com.schemafy.domain.project.application.port.in.RemoveProjectMemberUseCase;
import com.schemafy.domain.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.domain.project.application.port.in.RevokeShareLinkUseCase;
import com.schemafy.domain.project.application.port.in.UpdateProjectCommand;
import com.schemafy.domain.project.application.port.in.UpdateProjectMemberRoleCommand;
import com.schemafy.domain.project.application.port.in.UpdateProjectUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectController {

  private final CreateProjectUseCase createProjectUseCase;
  private final GetProjectsUseCase getProjectsUseCase;
  private final GetProjectUseCase getProjectUseCase;
  private final UpdateProjectUseCase updateProjectUseCase;
  private final DeleteProjectUseCase deleteProjectUseCase;
  private final RemoveProjectMemberUseCase removeProjectMemberUseCase;
  private final LeaveProjectUseCase leaveProjectUseCase;
  private final CreateShareLinkUseCase createShareLinkUseCase;
  private final GetShareLinksUseCase getShareLinksUseCase;
  private final GetShareLinkUseCase getShareLinkUseCase;
  private final RevokeShareLinkUseCase revokeShareLinkUseCase;
  private final DeleteShareLinkUseCase deleteShareLinkUseCase;
  private final ProjectMemberOrchestrator projectMemberOrchestrator;

  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/projects")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ProjectResponse> createProject(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return createProjectUseCase.createProject(new CreateProjectCommand(
        workspaceId,
        request.name(),
        request.description(),
        userId))
        .map(ProjectResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/workspaces/{workspaceId}/projects")
  public Mono<PageResponse<ProjectSummaryResponse>> getProjects(
      @PathVariable String workspaceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
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

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/projects/{projectId}")
  public Mono<ProjectResponse> getProject(
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return getProjectUseCase.getProject(new GetProjectQuery(projectId, userId))
        .map(ProjectResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
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

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteProject(@PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return deleteProjectUseCase.deleteProject(new DeleteProjectCommand(
        projectId,
        userId));
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/projects/{projectId}/members")
  public Mono<PageResponse<ProjectMemberResponse>> getMembers(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectMemberOrchestrator.getMembers(new GetProjectMembersQuery(
        projectId,
        userId,
        page,
        size))
        .map(result -> result.map(ProjectMemberResponse::from));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
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

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(@PathVariable String projectId, @PathVariable String userId,
      Authentication authentication) {
    String requester = authentication.getName();
    return removeProjectMemberUseCase.removeProjectMember(
        new RemoveProjectMemberCommand(projectId, userId, requester));
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @DeleteMapping("/projects/{projectId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveProject(
      @PathVariable String projectId, Authentication authentication) {
    String userId = authentication.getName();
    return leaveProjectUseCase.leaveProject(new LeaveProjectCommand(
        projectId,
        userId));
  }

  // ========== ShareLink Management ==========

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/projects/{projectId}/share-links")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ShareLinkResponse> createShareLink(
      @PathVariable String version,
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return createShareLinkUseCase.createShareLink(new CreateShareLinkCommand(
        projectId,
        userId))
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl, version));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/projects/{projectId}/share-links")
  public Mono<PageResponse<ShareLinkResponse>> getShareLinks(
      @PathVariable String version,
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return getShareLinksUseCase.getShareLinks(new GetShareLinksQuery(
        projectId,
        userId,
        page,
        size))
        .map(result -> PageResponse.of(
            result.content().stream()
                .map(link -> ShareLinkResponse.of(link, baseUrl, version))
                .toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/projects/{projectId}/share-links/{shareLinkId}")
  public Mono<ShareLinkResponse> getShareLink(
      @PathVariable String version,
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return getShareLinkUseCase.getShareLink(new GetShareLinkQuery(
        projectId,
        shareLinkId,
        userId))
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl, version));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PatchMapping("/projects/{projectId}/share-links/{shareLinkId}/revoke")
  public Mono<ShareLinkResponse> revokeShareLink(
      @PathVariable String version,
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return revokeShareLinkUseCase.revokeShareLink(new RevokeShareLinkCommand(
        projectId,
        shareLinkId,
        userId))
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl, version));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}/share-links/{shareLinkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteShareLink(
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return deleteShareLinkUseCase.deleteShareLink(new DeleteShareLinkCommand(
        projectId,
        shareLinkId,
        userId));
  }

}

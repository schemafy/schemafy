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
import com.schemafy.core.project.service.ProjectService;
import com.schemafy.core.project.service.ShareLinkService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;
  private final ShareLinkService shareLinkService;

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
    return projectService.createProject(workspaceId, request.name(), request.description(), userId)
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
    return projectService.getProjects(workspaceId, userId, page, size)
        .map(result -> result.map(ProjectSummaryResponse::from));
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/projects/{projectId}")
  public Mono<ProjectResponse> getProject(
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.getProject(projectId, userId)
        .map(ProjectResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PutMapping("/projects/{projectId}")
  public Mono<ProjectResponse> updateProject(
      @PathVariable String projectId,
      @Valid @RequestBody UpdateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.updateProject(projectId, request.name(), request.description(), userId)
        .map(ProjectResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteProject(@PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.deleteProject(projectId, userId);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/projects/{projectId}/members")
  public Mono<PageResponse<ProjectMemberResponse>> getMembers(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.getMembers(projectId, userId, page, size)
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
    return projectService
        .updateMemberRole(projectId, userId, request.role(), requesterId)
        .map(ProjectMemberResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(@PathVariable String projectId, @PathVariable String userId,
      Authentication authentication) {
    String requester = authentication.getName();
    return projectService.removeMember(projectId, userId,
        requester);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @DeleteMapping("/projects/{projectId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveProject(
      @PathVariable String projectId, Authentication authentication) {
    String userId = authentication.getName();
    return projectService.leaveProject(projectId, userId);
  }

  // ========== ShareLink Management ==========

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/projects/{projectId}/share-links")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ShareLinkResponse> createShareLink(
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.createShareLink(projectId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/projects/{projectId}/share-links")
  public Mono<PageResponse<ShareLinkResponse>> getShareLinks(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    int offset = page * size;
    return shareLinkService.countShareLinks(projectId, userId)
        .flatMap(total -> shareLinkService.getShareLinks(projectId, userId, size, offset)
            .map(link -> ShareLinkResponse.of(link, baseUrl))
            .collectList()
            .map(result -> PageResponse.of(result, page, size, total)));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/projects/{projectId}/share-links/{shareLinkId}")
  public Mono<ShareLinkResponse> getShareLink(
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.getShareLink(projectId, shareLinkId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PatchMapping("/projects/{projectId}/share-links/{shareLinkId}/revoke")
  public Mono<ShareLinkResponse> revokeShareLink(
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.revokeShareLink(projectId, shareLinkId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/projects/{projectId}/share-links/{shareLinkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteShareLink(
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.deleteShareLink(projectId, shareLinkId, userId);
  }

}

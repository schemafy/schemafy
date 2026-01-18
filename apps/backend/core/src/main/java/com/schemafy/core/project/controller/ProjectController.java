package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
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

  @Value("${app.base-url:https://schemafy.com}")
  private String baseUrl;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/projects")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<ProjectResponse>> createProject(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.createProject(workspaceId, request, userId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/workspaces/{workspaceId}/projects")
  public Mono<BaseResponse<PageResponse<ProjectSummaryResponse>>> getProjects(
      @PathVariable String workspaceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.getProjects(workspaceId, userId, page, size)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/workspaces/{workspaceId}/projects/{id}")
  public Mono<BaseResponse<ProjectResponse>> getProject(
      @PathVariable String workspaceId, @PathVariable String id,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.getProject(workspaceId, id, userId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PutMapping("/workspaces/{workspaceId}/projects/{id}")
  public Mono<BaseResponse<ProjectResponse>> updateProject(
      @PathVariable String workspaceId, @PathVariable String id,
      @Valid @RequestBody UpdateProjectRequest request,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.updateProject(workspaceId, id, request, userId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteProject(@PathVariable String workspaceId,
      @PathVariable String id, Authentication authentication) {
    String userId = authentication.getName();
    return projectService.deleteProject(workspaceId, id, userId);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @GetMapping("/workspaces/{workspaceId}/projects/{id}/members")
  public Mono<BaseResponse<PageResponse<ProjectMemberResponse>>> getMembers(
      @PathVariable String workspaceId, @PathVariable String id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    return projectService.getMembers(id, userId, page, size)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}/members/{memberId}/role")
  public Mono<BaseResponse<ProjectMemberResponse>> updateMemberRole(
      @PathVariable String workspaceId, @PathVariable String projectId,
      @PathVariable String memberId,
      @Valid @RequestBody UpdateProjectMemberRoleRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return projectService
        .updateMemberRole(projectId, memberId, request, requesterId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/members/{memberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(@PathVariable String workspaceId,
      @PathVariable String projectId, @PathVariable String memberId,
      Authentication authentication) {
    String requester = authentication.getName();
    return projectService.removeMember(projectId, memberId,
        requester);
  }

  @PreAuthorize("hasAnyRole('ADMIN','EDITOR','VIEWER')")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveProject(@PathVariable String workspaceId,
      @PathVariable String projectId, Authentication authentication) {
    String userId = authentication.getName();
    return projectService.leaveProject(projectId, userId);
  }

  // ========== ShareLink Management ==========

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<ShareLinkResponse>> createShareLink(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.createShareLink(workspaceId, projectId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links")
  public Mono<BaseResponse<PageResponse<ShareLinkResponse>>> getShareLinks(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication authentication) {
    String userId = authentication.getName();
    int offset = page * size;
    return shareLinkService.countShareLinks(workspaceId, projectId, userId)
        .flatMap(total -> shareLinkService.getShareLinks(workspaceId, projectId, userId, size, offset)
            .map(link -> ShareLinkResponse.of(link, baseUrl))
            .collectList()
            .map(list -> PageResponse.of(list, page, size, total)))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}")
  public Mono<BaseResponse<ShareLinkResponse>> getShareLink(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.getShareLink(workspaceId, projectId, shareLinkId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}/revoke")
  public Mono<BaseResponse<ShareLinkResponse>> revokeShareLink(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.revokeShareLink(workspaceId, projectId, shareLinkId, userId)
        .map(shareLink -> ShareLinkResponse.of(shareLink, baseUrl))
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/share-links/{shareLinkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteShareLink(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @PathVariable String shareLinkId,
      Authentication authentication) {
    String userId = authentication.getName();
    return shareLinkService.deleteShareLink(workspaceId, projectId, shareLinkId, userId);
  }

}

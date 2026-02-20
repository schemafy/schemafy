package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.core.project.controller.dto.response.ProjectInvitationCreateResponse;
import com.schemafy.core.project.controller.dto.response.ProjectInvitationResponse;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.service.ProjectInvitationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectInvitationController {

  private final ProjectInvitationService invitationService;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<ProjectInvitationCreateResponse>> createInvitation(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @Valid @RequestBody CreateProjectInvitationRequest request,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.createInvitation(
        workspaceId, projectId, request.email(), request.role(), currentUserId)
        .map(ProjectInvitationCreateResponse::of)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/invitations")
  public Mono<BaseResponse<PageResponse<ProjectInvitationResponse>>> listInvitations(
      @PathVariable String workspaceId,
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.getInvitations(
        workspaceId, projectId, currentUserId, page, size)
        .map(result -> result.map(ProjectInvitationResponse::of))
        .map(BaseResponse::success);
  }

  @GetMapping("/users/me/invitations/projects")
  public Mono<BaseResponse<PageResponse<ProjectInvitationResponse>>> listMyInvitations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.getMyInvitations(currentUserId, page, size)
        .map(result -> result.map(ProjectInvitationResponse::of))
        .map(BaseResponse::success);
  }

  @PatchMapping("/projects/invitations/{invitationId}/accept")
  public Mono<BaseResponse<ProjectMemberResponse>> acceptInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.acceptInvitation(invitationId, currentUserId)
        .map(ProjectMemberResponse::from)
        .map(BaseResponse::success);
  }

  @PatchMapping("/projects/invitations/{invitationId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> rejectInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.rejectInvitation(invitationId, currentUserId);
  }

}

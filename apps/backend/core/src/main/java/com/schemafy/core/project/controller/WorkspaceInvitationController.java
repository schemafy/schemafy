package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceInvitationCreateResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceInvitationResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.service.WorkspaceInvitationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class WorkspaceInvitationController {

  private final WorkspaceInvitationService invitationService;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<WorkspaceInvitationCreateResponse>> createInvitation(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateWorkspaceInvitationRequest request,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.createInvitation(
        workspaceId, request, currentUserId)
        .map(WorkspaceInvitationCreateResponse::of)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/invitations")
  public Mono<BaseResponse<PageResponse<WorkspaceInvitationResponse>>> getInvitations(
      @PathVariable String workspaceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.listInvitations(
        workspaceId, currentUserId, page, size)
        .map(BaseResponse::success);
  }

  @GetMapping("/users/me/invitations/workspaces")
  public Mono<BaseResponse<PageResponse<WorkspaceInvitationResponse>>> getMyInvitations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.listMyInvitations(currentUserId, page, size)
        .map(BaseResponse::success);
  }

  @PutMapping("/workspaces/invitations/{invitationId}/accept")
  public Mono<BaseResponse<WorkspaceMemberResponse>> acceptInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.acceptInvitation(invitationId, currentUserId)
        .map(BaseResponse::success);
  }

  @PutMapping("/workspaces/invitations/{invitationId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> rejectInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return invitationService.rejectInvitation(invitationId, currentUserId);
  }

}

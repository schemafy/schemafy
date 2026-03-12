package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceInvitationRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceInvitationCreateResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceInvitationResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.orchestrator.WorkspaceMemberOrchestrator;
import com.schemafy.domain.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.domain.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.domain.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.domain.project.application.port.in.GetMyWorkspaceInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetMyWorkspaceInvitationsUseCase;
import com.schemafy.domain.project.application.port.in.GetWorkspaceInvitationsQuery;
import com.schemafy.domain.project.application.port.in.GetWorkspaceInvitationsUseCase;
import com.schemafy.domain.project.application.port.in.RejectWorkspaceInvitationCommand;
import com.schemafy.domain.project.application.port.in.RejectWorkspaceInvitationUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class WorkspaceInvitationController {

  private final CreateWorkspaceInvitationUseCase createWorkspaceInvitationUseCase;
  private final GetWorkspaceInvitationsUseCase getWorkspaceInvitationsUseCase;
  private final GetMyWorkspaceInvitationsUseCase getMyWorkspaceInvitationsUseCase;
  private final RejectWorkspaceInvitationUseCase rejectWorkspaceInvitationUseCase;
  private final WorkspaceMemberOrchestrator workspaceMemberOrchestrator;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<WorkspaceInvitationCreateResponse> createInvitation(
      @PathVariable String workspaceId,
      @Valid @RequestBody CreateWorkspaceInvitationRequest request,
      Authentication auth) {
    String currentUserId = auth.getName();
    return createWorkspaceInvitationUseCase.createWorkspaceInvitation(
        new CreateWorkspaceInvitationCommand(
            workspaceId,
            request.email(),
            request.role(),
            currentUserId))
        .map(WorkspaceInvitationCreateResponse::of);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/workspaces/{workspaceId}/invitations")
  public Mono<PageResponse<WorkspaceInvitationResponse>> getInvitations(
      @PathVariable String workspaceId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return getWorkspaceInvitationsUseCase.getWorkspaceInvitations(
        new GetWorkspaceInvitationsQuery(
            workspaceId,
            currentUserId,
            page,
            size))
        .map(result -> PageResponse.of(
            result.content().stream().map(WorkspaceInvitationResponse::of).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @GetMapping("/users/me/invitations/workspaces")
  public Mono<PageResponse<WorkspaceInvitationResponse>> getMyInvitations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return getMyWorkspaceInvitationsUseCase.getMyWorkspaceInvitations(
        new GetMyWorkspaceInvitationsQuery(currentUserId, page, size))
        .map(result -> PageResponse.of(
            result.content().stream().map(WorkspaceInvitationResponse::of).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @PatchMapping("/workspaces/invitations/{invitationId}/accept")
  public Mono<WorkspaceMemberResponse> acceptInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return workspaceMemberOrchestrator.acceptInvitation(
        new AcceptWorkspaceInvitationCommand(invitationId, currentUserId))
        .map(WorkspaceMemberResponse::from);
  }

  @PatchMapping("/workspaces/invitations/{invitationId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> rejectInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return rejectWorkspaceInvitationUseCase.rejectWorkspaceInvitation(
        new RejectWorkspaceInvitationCommand(invitationId, currentUserId));
  }

}

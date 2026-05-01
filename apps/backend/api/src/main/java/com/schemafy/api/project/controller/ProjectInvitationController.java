package com.schemafy.api.project.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.schemafy.api.common.constant.ApiPath;
import com.schemafy.api.common.type.PageResponse;
import com.schemafy.api.project.controller.dto.request.CreateProjectInvitationRequest;
import com.schemafy.api.project.controller.dto.response.ProjectInvitationCreateResponse;
import com.schemafy.api.project.controller.dto.response.ProjectInvitationResponse;
import com.schemafy.api.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.api.project.orchestrator.ProjectMemberOrchestrator;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetMyProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsQuery;
import com.schemafy.core.project.application.port.in.GetProjectInvitationsUseCase;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectProjectInvitationUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectInvitationController {

  private final CreateProjectInvitationUseCase createProjectInvitationUseCase;
  private final GetProjectInvitationsUseCase getProjectInvitationsUseCase;
  private final GetMyProjectInvitationsUseCase getMyProjectInvitationsUseCase;
  private final RejectProjectInvitationUseCase rejectProjectInvitationUseCase;
  private final ProjectMemberOrchestrator projectMemberOrchestrator;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/projects/{projectId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ProjectInvitationCreateResponse> createInvitation(
      @PathVariable String projectId,
      @Valid @RequestBody CreateProjectInvitationRequest request,
      Authentication auth) {
    String currentUserId = auth.getName();
    return createProjectInvitationUseCase.createProjectInvitation(
        new CreateProjectInvitationCommand(
            projectId,
            request.email(),
            request.role(),
            currentUserId))
        .map(ProjectInvitationCreateResponse::of);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/projects/{projectId}/invitations")
  public Mono<PageResponse<ProjectInvitationResponse>> listInvitations(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "10") @Positive @Max(100) int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return getProjectInvitationsUseCase.getProjectInvitations(
        new GetProjectInvitationsQuery(
            projectId,
            currentUserId,
            page,
            size))
        .map(result -> PageResponse.of(
            result.content().stream().map(ProjectInvitationResponse::of).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @GetMapping("/users/me/invitations/projects")
  public Mono<PageResponse<ProjectInvitationResponse>> listMyInvitations(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "10") @Positive @Max(100) int size,
      Authentication auth) {
    String currentUserId = auth.getName();
    return getMyProjectInvitationsUseCase.getMyProjectInvitations(
        new GetMyProjectInvitationsQuery(currentUserId, page, size))
        .map(result -> PageResponse.of(
            result.content().stream().map(ProjectInvitationResponse::of).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @PatchMapping("/projects/invitations/{invitationId}/accept")
  public Mono<ProjectMemberResponse> acceptInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return projectMemberOrchestrator.acceptInvitation(
        new AcceptProjectInvitationCommand(invitationId, currentUserId))
        .map(ProjectMemberResponse::from);
  }

  @PatchMapping("/projects/invitations/{invitationId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> rejectInvitation(
      @PathVariable String invitationId,
      Authentication auth) {
    String currentUserId = auth.getName();
    return rejectProjectInvitationUseCase.rejectProjectInvitation(
        new RejectProjectInvitationCommand(invitationId, currentUserId));
  }

}

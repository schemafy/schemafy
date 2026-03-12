package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.project.orchestrator.WorkspaceMemberOrchestrator;
import com.schemafy.domain.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.domain.project.application.port.in.CreateWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.CreateWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.DeleteWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.DeleteWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.GetWorkspaceQuery;
import com.schemafy.domain.project.application.port.in.GetWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.GetWorkspaceMembersQuery;
import com.schemafy.domain.project.application.port.in.GetWorkspacesQuery;
import com.schemafy.domain.project.application.port.in.GetWorkspacesUseCase;
import com.schemafy.domain.project.application.port.in.LeaveWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.LeaveWorkspaceUseCase;
import com.schemafy.domain.project.application.port.in.RemoveWorkspaceMemberCommand;
import com.schemafy.domain.project.application.port.in.RemoveWorkspaceMemberUseCase;
import com.schemafy.domain.project.application.port.in.UpdateWorkspaceCommand;
import com.schemafy.domain.project.application.port.in.UpdateWorkspaceMemberRoleCommand;
import com.schemafy.domain.project.application.port.in.UpdateWorkspaceUseCase;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class WorkspaceController {

  private final CreateWorkspaceUseCase createWorkspaceUseCase;
  private final GetWorkspacesUseCase getWorkspacesUseCase;
  private final GetWorkspaceUseCase getWorkspaceUseCase;
  private final UpdateWorkspaceUseCase updateWorkspaceUseCase;
  private final DeleteWorkspaceUseCase deleteWorkspaceUseCase;
  private final RemoveWorkspaceMemberUseCase removeWorkspaceMemberUseCase;
  private final LeaveWorkspaceUseCase leaveWorkspaceUseCase;
  private final WorkspaceMemberOrchestrator workspaceMemberOrchestrator;

  @PostMapping("/workspaces")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<WorkspaceResponse> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return createWorkspaceUseCase.createWorkspace(new CreateWorkspaceCommand(
        request.name(),
        request.description(),
        requesterId))
        .map(WorkspaceResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces")
  public Mono<PageResponse<WorkspaceSummaryResponse>> getWorkspaces(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return getWorkspacesUseCase.getWorkspaces(new GetWorkspacesQuery(
        requesterId,
        page,
        size))
        .map(result -> PageResponse.of(
            result.content().stream().map(WorkspaceSummaryResponse::of).toList(),
            result.page(),
            result.size(),
            result.totalElements()));
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces/{id}")
  public Mono<WorkspaceResponse> getWorkspace(
      @PathVariable String id, Authentication authentication) {
    String requesterId = authentication.getName();
    return getWorkspaceUseCase.getWorkspace(new GetWorkspaceQuery(id,
        requesterId))
        .map(WorkspaceResponse::from);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PutMapping("/workspaces/{id}")
  public Mono<WorkspaceResponse> updateWorkspace(
      @PathVariable String id,
      @Valid @RequestBody UpdateWorkspaceRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return updateWorkspaceUseCase.updateWorkspace(new UpdateWorkspaceCommand(
        id,
        request.name(),
        request.description(),
        requesterId))
        .map(WorkspaceResponse::from);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/workspaces/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteWorkspace(@PathVariable String id,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return deleteWorkspaceUseCase.deleteWorkspace(new DeleteWorkspaceCommand(
        id,
        requesterId));
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces/{id}/members")
  public Mono<PageResponse<WorkspaceMemberResponse>> getMembers(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceMemberOrchestrator.getMembers(new GetWorkspaceMembersQuery(
        id,
        requesterId,
        page,
        size))
        .map(result -> result.map(WorkspaceMemberResponse::from));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<WorkspaceMemberResponse> addMember(
      @PathVariable String workspaceId,
      @Valid @RequestBody AddWorkspaceMemberRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceMemberOrchestrator.addMember(new AddWorkspaceMemberCommand(
        workspaceId,
        request.email(),
        request.role(),
        requesterId))
        .map(WorkspaceMemberResponse::from);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(
      @PathVariable String workspaceId,
      @PathVariable String userId,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return removeWorkspaceMemberUseCase.removeWorkspaceMember(
        new RemoveWorkspaceMemberCommand(
            workspaceId,
            userId,
            requesterId));
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @DeleteMapping("/workspaces/{workspaceId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveMember(
      @PathVariable String workspaceId,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return leaveWorkspaceUseCase.leaveWorkspace(new LeaveWorkspaceCommand(
        workspaceId,
        requesterId));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/workspaces/{workspaceId}/members/{userId}/role")
  public Mono<WorkspaceMemberResponse> updateMemberRole(
      @PathVariable String workspaceId,
      @PathVariable String userId,
      @Valid @RequestBody UpdateMemberRoleRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceMemberOrchestrator.updateMemberRole(
        new UpdateWorkspaceMemberRoleCommand(
            workspaceId,
            userId,
            request.role(),
            requesterId))
        .map(WorkspaceMemberResponse::from);
  }

}

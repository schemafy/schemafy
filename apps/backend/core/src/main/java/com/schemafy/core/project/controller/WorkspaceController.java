package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.project.service.WorkspaceService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  @PostMapping("/workspaces")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<WorkspaceResponse>> createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.createWorkspace(request, requesterId)
        .map(WorkspaceResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces")
  public Mono<BaseResponse<PageResponse<WorkspaceSummaryResponse>>> getWorkspaces(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.getWorkspaces(requesterId, page, size)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces/{id}")
  public Mono<BaseResponse<WorkspaceResponse>> getWorkspace(
      @PathVariable String id, Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.getWorkspace(id, requesterId)
        .map(WorkspaceResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PutMapping("/workspaces/{id}")
  public Mono<BaseResponse<WorkspaceResponse>> updateWorkspace(
      @PathVariable String id,
      @Valid @RequestBody UpdateWorkspaceRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.updateWorkspace(id, request, requesterId)
        .map(WorkspaceResponse::from)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/workspaces/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteWorkspace(@PathVariable String id,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.deleteWorkspace(id, requesterId);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @GetMapping("/workspaces/{id}/members")
  public Mono<BaseResponse<PageResponse<WorkspaceMemberResponse>>> getMembers(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.getMembers(id, requesterId, page, size)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/workspaces/{workspaceId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BaseResponse<WorkspaceMemberResponse>> addMember(
      @PathVariable String workspaceId,
      @Valid @RequestBody AddWorkspaceMemberRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.addMember(workspaceId, request, requesterId)
        .map(BaseResponse::success);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/workspaces/{workspaceId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removeMember(
      @PathVariable String workspaceId,
      @PathVariable String userId,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.removeMember(workspaceId, userId,
        requesterId);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
  @DeleteMapping("/workspaces/{workspaceId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> leaveMember(
      @PathVariable String workspaceId,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService.leaveMember(workspaceId, requesterId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/workspaces/{workspaceId}/members/{userId}/role")
  public Mono<BaseResponse<WorkspaceMemberResponse>> updateMemberRole(
      @PathVariable String workspaceId,
      @PathVariable String userId,
      @Valid @RequestBody UpdateMemberRoleRequest request,
      Authentication authentication) {
    String requesterId = authentication.getName();
    return workspaceService
        .updateMemberRole(workspaceId, userId, request, requesterId)
        .map(BaseResponse::success);
  }

}

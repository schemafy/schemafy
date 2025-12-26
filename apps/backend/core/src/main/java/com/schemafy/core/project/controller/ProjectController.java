package com.schemafy.core.project.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateProjectRequest;
import com.schemafy.core.project.controller.dto.request.JoinProjectByShareLinkRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.controller.dto.response.ProjectResponse;
import com.schemafy.core.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API)
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
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

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
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

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/workspaces/{workspaceId}/projects/{id}")
    public Mono<BaseResponse<ProjectResponse>> getProject(
            @PathVariable String workspaceId, @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.getProject(workspaceId, id, userId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PutMapping("/workspaces/{workspaceId}/projects/{id}")
    public Mono<BaseResponse<ProjectResponse>> updateProject(
            @PathVariable String workspaceId, @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.updateProject(workspaceId, id, request, userId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @DeleteMapping("/workspaces/{workspaceId}/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProject(@PathVariable String workspaceId,
            @PathVariable String id, Authentication authentication) {
        String userId = authentication.getName();
        return projectService.deleteProject(workspaceId, id, userId);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/workspaces/{workspaceId}/projects/{id}/members")
    public Mono<BaseResponse<PageResponse<ProjectMemberResponse>>> getMembers(
            @PathVariable String workspaceId, @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.getMembers(workspaceId, id, userId, page, size)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @PostMapping("/projects/join")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BaseResponse<ProjectMemberResponse>> joinProjectByShareLink(
            @Valid @RequestBody JoinProjectByShareLinkRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.joinProjectByShareLink(request.token(), userId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}/members/{memberId}/role")
    public Mono<BaseResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable String workspaceId, @PathVariable String projectId,
            @PathVariable String memberId,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request,
            Authentication authentication) {
        String requesterId = authentication.getName();
        return projectService
                .updateMemberRole(workspaceId, projectId, memberId, request,
                        requesterId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeMember(@PathVariable String workspaceId,
            @PathVariable String projectId, @PathVariable String memberId,
            Authentication authentication) {
        String requester = authentication.getName();
        return projectService.removeMember(workspaceId, projectId, memberId,
                requester);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> leaveProject(@PathVariable String workspaceId,
            @PathVariable String projectId, Authentication authentication) {
        String userId = authentication.getName();
        return projectService.leaveProject(workspaceId, projectId, userId);
    }

}

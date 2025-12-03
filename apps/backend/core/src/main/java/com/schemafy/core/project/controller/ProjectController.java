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
import com.schemafy.core.project.controller.dto.request.UpdateProjectRequest;
import com.schemafy.core.project.controller.dto.response.ProjectMemberResponse;
import com.schemafy.core.project.controller.dto.response.ProjectResponse;
import com.schemafy.core.project.controller.dto.response.ProjectSummaryResponse;
import com.schemafy.core.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(ApiPath.API + "/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PostMapping
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
    @GetMapping
    public Mono<BaseResponse<PageResponse<ProjectSummaryResponse>>> getProjects(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.getProjects(workspaceId, userId, page, size)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/{id}")
    public Mono<BaseResponse<ProjectResponse>> getProject(
            @PathVariable String workspaceId, @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.getProject(workspaceId, id, userId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @PutMapping("/{id}")
    public Mono<BaseResponse<ProjectResponse>> updateProject(
            @PathVariable String workspaceId, @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.updateProject(workspaceId, id, request, userId)
                .map(BaseResponse::success);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProject(@PathVariable String workspaceId,
            @PathVariable String id, Authentication authentication) {
        String userId = authentication.getName();
        return projectService.deleteProject(workspaceId, id, userId);
    }

    @PreAuthorize("hasAnyRole('OWNER','ADMIN','EDITOR','COMMENTER','VIEWER')")
    @GetMapping("/{id}/members")
    public Mono<BaseResponse<PageResponse<ProjectMemberResponse>>> getMembers(
            @PathVariable String workspaceId, @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        return projectService.getMembers(workspaceId, id, userId, page, size)
                .map(BaseResponse::success);
    }

}

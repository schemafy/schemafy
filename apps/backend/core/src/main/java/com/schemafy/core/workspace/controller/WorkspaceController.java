package com.schemafy.core.workspace.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schemafy.core.common.constant.ApiPath;
import com.schemafy.core.common.type.BaseResponse;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.workspace.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.workspace.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.workspace.service.WorkspaceService;

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
        String userId = authentication.getName();
        return workspaceService.createWorkspace(request, userId)
                .map(BaseResponse::success);
    }

    @GetMapping("/workspaces")
    public Mono<BaseResponse<PageResponse<WorkspaceSummaryResponse>>> getWorkspaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        return workspaceService.getWorkspaces(userId, page, size)
                .map(BaseResponse::success);
    }

    @GetMapping("/workspaces/{id}")
    public Mono<BaseResponse<WorkspaceResponse>> getWorkspace(
            @PathVariable String id, Authentication authentication) {
        String userId = authentication.getName();
        return workspaceService.getWorkspace(id, userId)
                .map(BaseResponse::success);
    }

    @PutMapping("/workspaces/{id}")
    public Mono<BaseResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable String id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return workspaceService.updateWorkspace(id, request, userId)
                .map(BaseResponse::success);
    }

    @DeleteMapping("/workspaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteWorkspace(@PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        return workspaceService.deleteWorkspace(id, userId);
    }

    @GetMapping("/workspaces/{id}/members")
    public Mono<BaseResponse<PageResponse<WorkspaceMemberResponse>>> getMembers(
            @PathVariable String id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        return workspaceService.getMembers(id, userId, page, size)
                .map(BaseResponse::success);
    }
}

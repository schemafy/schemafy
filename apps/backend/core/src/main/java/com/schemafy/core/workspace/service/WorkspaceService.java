package com.schemafy.core.workspace.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.user.repository.UserRepository;
import com.schemafy.core.workspace.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.workspace.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.workspace.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.workspace.repository.WorkspaceMemberRepository;
import com.schemafy.core.workspace.repository.WorkspaceRepository;
import com.schemafy.core.workspace.repository.entity.Workspace;
import com.schemafy.core.workspace.repository.entity.WorkspaceMember;
import com.schemafy.core.workspace.repository.vo.WorkspaceRole;
import com.schemafy.core.workspace.repository.vo.WorkspaceSettings;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Mono<WorkspaceResponse> createWorkspace(
            CreateWorkspaceRequest request, String userId) {
        return Mono.defer(() -> {
            WorkspaceSettings settings = request.getSettingsOrDefault();
            validateSettings(settings);

            Workspace workspace = Workspace.create(userId, request.name(),
                    request.description(), settings);

            WorkspaceMember ownerMember = WorkspaceMember.create(
                    workspace.getId(), userId, WorkspaceRole.ADMIN);

            return workspaceRepository.save(workspace).flatMap(
                    savedWorkspace -> workspaceMemberRepository.save(
                            ownerMember).thenReturn(savedWorkspace))
                    .map(WorkspaceResponse::from);
        });
    }

    @Transactional(readOnly = true)
    public Mono<PageResponse<WorkspaceSummaryResponse>> getWorkspaces(
            String userId, int page, int size) {
        return workspaceMemberRepository.findByUserIdAndNotDeleted(userId)
                .flatMap(member -> workspaceRepository.findByIdAndNotDeleted(
                        member.getWorkspaceId()).flatMap(
                                workspace -> workspaceMemberRepository
                                        .countByWorkspaceIdAndNotDeleted(
                                                workspace.getId())
                                        .map(count -> new WorkspaceSummaryResponse(
                                                workspace.getId(),
                                                workspace.getName(),
                                                workspace.getDescription(),
                                                workspace.getOwnerId(),
                                                member.getRole(), count,
                                                workspace.getCreatedAt(),
                                                workspace.getUpdatedAt()))))
                .collectList().flatMap(allWorkspaces -> {
                    int offset = page * size;
                    int totalElements = allWorkspaces.size();
                    int start = Math.min(offset, totalElements);
                    int end = Math.min(offset + size, totalElements);
                    List<WorkspaceSummaryResponse> pagedContent = allWorkspaces
                            .subList(
                                    start, end);
                    return Mono.just(PageResponse.of(pagedContent, page, size,
                            totalElements));
                });
    }

    @Transactional(readOnly = true)
    public Mono<WorkspaceResponse> getWorkspace(String workspaceId,
            String userId) {
        return validateMemberAccess(workspaceId, userId).then(
                workspaceRepository.findByIdAndNotDeleted(workspaceId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)))
                .map(WorkspaceResponse::from);
    }

    @Transactional
    public Mono<WorkspaceResponse> updateWorkspace(String workspaceId,
            UpdateWorkspaceRequest request, String userId) {
        return validateOwnerAccess(workspaceId, userId).then(
                workspaceRepository.findByIdAndNotDeleted(workspaceId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)))
                .flatMap(workspace -> {
                    WorkspaceSettings settings = request.getSettingsOrDefault();
                    validateSettings(settings);

                    workspace.update(request.name(), request.description(),
                            settings);
                    return workspaceRepository.save(workspace);
                }).map(WorkspaceResponse::from);
    }

    @Transactional
    public Mono<Void> deleteWorkspace(String workspaceId, String userId) {
        return validateOwnerAccess(workspaceId, userId).then(
                workspaceRepository.findByIdAndNotDeleted(workspaceId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)))
                .flatMap(workspace -> {
                    if (workspace.isDeleted()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ALREADY_DELETED));
                    }
                    workspace.delete();
                    return workspaceRepository.save(workspace)
                            .then(workspaceMemberRepository
                                    .softDeleteByWorkspaceId(
                                            workspaceId));
                });
    }

    @Transactional(readOnly = true)
    public Mono<PageResponse<WorkspaceMemberResponse>> getMembers(
            String workspaceId, String userId, int page, int size) {
        return validateAdminAccess(workspaceId, userId).then(
                workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
                        workspaceId))
                .flatMap(totalElements -> {
                    int offset = page * size;
                    return workspaceMemberRepository
                            .findByWorkspaceIdAndNotDeleted(
                                    workspaceId, size, offset)
                            .flatMap(
                                    member -> userRepository
                                            .findById(member.getUserId())
                                            .map(user -> new WorkspaceMemberResponse(
                                                    member.getId(),
                                                    user.getId(),
                                                    user.getName(),
                                                    user.getEmail(),
                                                    member.getRole(),
                                                    member.getJoinedAt())))
                            .collectList()
                            .map(members -> PageResponse.of(members, page, size,
                                    totalElements));
                });
    }

    private Mono<Void> validateMemberAccess(String workspaceId, String userId) {
        return workspaceMemberRepository
                .existsByWorkspaceIdAndUserIdAndNotDeleted(
                        workspaceId, userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateOwnerAccess(String workspaceId, String userId) {
        return workspaceRepository.findByIdAndNotDeleted(workspaceId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)))
                .flatMap(workspace -> {
                    if (!workspace.isOwner(userId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_OWNER_ONLY));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateAdminAccess(String workspaceId, String userId) {
        return workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndNotDeleted(
                        workspaceId, userId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.WORKSPACE_ACCESS_DENIED)))
                .flatMap(member -> {
                    if (!member.isAdmin()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ADMIN_REQUIRED));
                    }
                    return Mono.empty();
                });
    }

    private void validateSettings(WorkspaceSettings settings) {
        settings.validate();
        String json = settings.toJson();
        if (json.length() > 65536) {
            throw new BusinessException(ErrorCode.WORKSPACE_SETTINGS_TOO_LARGE);
        }
    }
}

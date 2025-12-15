package com.schemafy.core.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.AddWorkspaceMemberRequest;
import com.schemafy.core.project.controller.dto.request.CreateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.request.UpdateMemberRoleRequest;
import com.schemafy.core.project.controller.dto.request.UpdateWorkspaceRequest;
import com.schemafy.core.project.controller.dto.response.WorkspaceMemberResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceResponse;
import com.schemafy.core.project.controller.dto.response.WorkspaceSummaryResponse;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.WorkspaceRepository;
import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.entity.WorkspaceMember;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;
import com.schemafy.core.user.repository.UserRepository;

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
                                        .map(memberCount -> WorkspaceSummaryResponse
                                                .of(workspace, memberCount))))
                .collectList().flatMap(allWorkspaces -> {
                    int offset = page * size;
                    int totalElements = allWorkspaces.size();
                    int start = Math.min(offset, totalElements);
                    int end = Math.min(offset + size, totalElements);
                    List<WorkspaceSummaryResponse> pagedContent = allWorkspaces
                            .subList(start, end);
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
        return validateAdminAccess(workspaceId, userId).then(
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
        return validateAdminAccess(workspaceId, userId).then(
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
        return validateMemberAccess(workspaceId, userId).then(
                workspaceMemberRepository.countByWorkspaceIdAndNotDeleted(
                        workspaceId))
                .flatMap(totalElements -> {
                    int offset = page * size;
                    return workspaceMemberRepository
                            .findByWorkspaceIdAndNotDeleted(
                                    workspaceId, size, offset)
                            .flatMap(
                                    workSpaceMember -> userRepository
                                            .findById(
                                                    workSpaceMember.getUserId())
                                            .map(user -> WorkspaceMemberResponse
                                                    .of(workSpaceMember, user)))
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

    /**
     * 워크스페이스에 멤버 추가
     */
    @Transactional
    public Mono<WorkspaceMemberResponse> addMember(
            String workspaceId,
            AddWorkspaceMemberRequest request,
            String currentUserId) {
        return validateAdminAccess(workspaceId, currentUserId)
                .then(userRepository.findById(request.userId())
                        .switchIfEmpty(Mono.error(new BusinessException(
                                ErrorCode.USER_NOT_FOUND))))
                .then(workspaceMemberRepository
                        .existsByWorkspaceIdAndUserIdAndNotDeleted(
                                workspaceId, request.userId()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.MEMBER_ALREADY_EXISTS));
                    }
                    return workspaceMemberRepository
                            .countByWorkspaceIdAndNotDeleted(workspaceId);
                })
                .flatMap(memberCount -> {
                    if (memberCount >= 100) {
                        return Mono.error(new BusinessException(
                                ErrorCode.MEMBER_LIMIT_EXCEEDED));
                    }
                    WorkspaceMember newMember = WorkspaceMember.create(
                            workspaceId, request.userId(), request.role());
                    return workspaceMemberRepository.save(newMember);
                })
                .flatMap(savedMember -> userRepository
                        .findById(savedMember.getUserId())
                        .map(user -> WorkspaceMemberResponse.of(savedMember,
                                user)));
    }

    /**
     * 워크스페이스 멤버 제거
     */
    @Transactional
    public Mono<Void> removeMember(
            String workspaceId,
            String memberId,
            String currentUserId) {
        return validateAdminAccess(workspaceId, currentUserId)
                .then(workspaceMemberRepository.findByIdAndNotDeleted(memberId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(targetMember -> {
                    if (targetMember.isAdmin()) {
                        return workspaceMemberRepository
                                .countByWorkspaceIdAndRoleAndNotDeleted(
                                        workspaceId,
                                        WorkspaceRole.ADMIN.getValue())
                                .flatMap(adminCount -> {
                                    if (adminCount <= 1) {
                                        return Mono.error(new BusinessException(
                                                ErrorCode.LAST_ADMIN_CANNOT_BE_REMOVED));
                                    }
                                    targetMember.delete();
                                    return workspaceMemberRepository
                                            .save(targetMember);
                                });
                    } else {
                        targetMember.delete();
                        return workspaceMemberRepository.save(targetMember);
                    }
                })
                .then();
    }

    /**
     * 본인 워크스페이스 탈퇴
     */
    @Transactional
    public Mono<Void> leaveMember(
            String workspaceId,
            String currentUserId) {
        return workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndNotDeleted(
                        workspaceId, currentUserId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(myMember -> workspaceMemberRepository
                        .countByWorkspaceIdAndNotDeleted(
                                workspaceId)
                        .flatMap(totalMembers -> {
                            // 멤버가 1명만 있으면 워크스페이스 삭제
                            if (totalMembers == 1) {
                                return this.deleteWorkspace(workspaceId,
                                        currentUserId);
                            }

                            // ADMIN이고 ADMIN이 1명만 남았으면 탈퇴 불가
                            if (myMember.isAdmin()) {
                                return workspaceMemberRepository
                                        .countByWorkspaceIdAndRoleAndNotDeleted(
                                                workspaceId,
                                                WorkspaceRole.ADMIN.getValue())
                                        .flatMap(adminCount -> {
                                            if (adminCount <= 1) {
                                                return Mono.error(
                                                        new BusinessException(
                                                                ErrorCode.LAST_ADMIN_CANNOT_LEAVE));
                                            }
                                            myMember.delete();
                                            return workspaceMemberRepository
                                                    .save(myMember).then();
                                        });
                            } else {
                                myMember.delete();
                                return workspaceMemberRepository.save(myMember)
                                        .then();
                            }
                        }));
    }

    /**
     * 멤버 권한 변경
     */
    @Transactional
    public Mono<WorkspaceMemberResponse> updateMemberRole(
            String workspaceId,
            String memberId,
            UpdateMemberRoleRequest request,
            String currentUserId) {
        return validateAdminAccess(workspaceId, currentUserId)
                .then(workspaceMemberRepository.findByIdAndNotDeleted(memberId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
                .flatMap(targetMember -> {
                    // ADMIN -> MEMBER 변경인 경우, 마지막 ADMIN 체크
                    if (targetMember.isAdmin()
                            && request.role() == WorkspaceRole.MEMBER) {
                        return workspaceMemberRepository
                                .countByWorkspaceIdAndRoleAndNotDeleted(
                                        workspaceId,
                                        WorkspaceRole.ADMIN.getValue())
                                .flatMap(adminCount -> {
                                    if (adminCount <= 1) {
                                        return Mono.error(new BusinessException(
                                                ErrorCode.LAST_ADMIN_CANNOT_CHANGE_ROLE));
                                    }
                                    targetMember.updateRole(request.role());
                                    return workspaceMemberRepository
                                            .save(targetMember);
                                });
                    } else {
                        targetMember.updateRole(request.role());
                        return workspaceMemberRepository.save(targetMember);
                    }
                })
                .flatMap(updatedMember -> userRepository
                        .findById(updatedMember.getUserId())
                        .map(user -> WorkspaceMemberResponse.of(updatedMember,
                                user)));
    }

}

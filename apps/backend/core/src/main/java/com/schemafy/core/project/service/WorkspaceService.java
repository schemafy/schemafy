package com.schemafy.core.project.service;

import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final Logger log = LoggerFactory
            .getLogger(WorkspaceService.class);
    private static final int WORKSPACE_MAX_MEMBERS_COUNT = 30;

    private final TransactionalOperator transactionalOperator;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

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
        }).as(transactionalOperator::transactional);
    }

    public Mono<PageResponse<WorkspaceSummaryResponse>> getWorkspaces(
            String userId, int page, int size) {
        int offset = page * size;
        return workspaceRepository.countByUserId(userId)
                .flatMap(totalElements -> workspaceRepository
                        .findByUserIdWithPaging(userId, size, offset)
                        .flatMap(workspace -> workspaceMemberRepository
                                .countByWorkspaceIdAndNotDeleted(
                                        workspace.getId())
                                .map(memberCount -> WorkspaceSummaryResponse
                                        .of(workspace, memberCount)))
                        .collectList()
                        .map(content -> PageResponse.of(content, page, size,
                                totalElements)));
    }

    public Mono<WorkspaceResponse> getWorkspace(String workspaceId,
            String userId) {
        return validateMemberAccess(workspaceId, userId).then(
                workspaceRepository.findByIdAndNotDeleted(workspaceId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND)))
                .map(WorkspaceResponse::from);
    }

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
                }).map(WorkspaceResponse::from)
                .as(transactionalOperator::transactional);
    }

    public Mono<Void> deleteWorkspace(String workspaceId, String userId) {
        return validateAdminAccess(workspaceId, userId)
                .then(workspaceRepository.findByIdAndNotDeleted(workspaceId))
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
                }).as(transactionalOperator::transactional);
    }

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
                            .flatMap(this::buildMemberResponse)
                            .collectList()
                            .map(members -> PageResponse.of(members, page, size,
                                    totalElements));
                });
    }

    /**
     * 워크스페이스에 멤버 추가
     * - Soft delete된 멤버는 재활성화
     * - DB UNIQUE constraint로 중복 방지
     */
    public Mono<WorkspaceMemberResponse> addMember(
            String workspaceId,
            AddWorkspaceMemberRequest request,
            String currentUserId) {

        return validateAdminAccess(workspaceId, currentUserId)
                .then(validateUserExists(request.userId()))
                .then(workspaceMemberRepository
                        .findLatestByWorkspaceIdAndUserId(workspaceId,
                                request.userId())
                        .flatMap(existing -> {
                            if (!existing.isDeleted()) {
                                log.warn(
                                        "Member already exists and is active: memberId={}",
                                        existing.getId());
                                return Mono.error(new BusinessException(
                                        ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS));
                            }

                            // 삭제된 멤버 재활성화
                            return workspaceMemberRepository
                                    .reactivateMember(existing.getId(),
                                            request.role().getValue())
                                    .then(workspaceMemberRepository
                                            .findById(existing.getId()));
                        })
                        .switchIfEmpty(Mono.defer(() ->
                        // 신규 멤버 생성
                        workspaceMemberRepository
                                .countByWorkspaceIdAndNotDeleted(workspaceId)
                                .flatMap(memberCount -> {
                                    if (memberCount >= WORKSPACE_MAX_MEMBERS_COUNT) {
                                        log.warn(
                                                "Workspace member limit exceeded: workspaceId={}",
                                                workspaceId);
                                        return Mono.error(new BusinessException(
                                                ErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED));
                                    }

                                    WorkspaceMember newMember = WorkspaceMember
                                            .create(
                                                    workspaceId,
                                                    request.userId(),
                                                    request.role());
                                    return workspaceMemberRepository
                                            .save(newMember);
                                }))))
                .flatMap(this::buildMemberResponse)
                .onErrorResume(error -> {
                    if (error instanceof DataIntegrityViolationException) {
                        log.warn(
                                "Duplicate key constraint: workspaceId={}, userId={}",
                                workspaceId, request.userId());
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_MEMBER_ALREADY_EXISTS));
                    }
                    return Mono.error(error);
                })
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> validateUserExists(String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono
                        .error(new BusinessException(ErrorCode.USER_NOT_FOUND)))
                .then();
    }

    private Mono<WorkspaceMemberResponse> buildMemberResponse(
            WorkspaceMember member) {
        return userRepository.findById(member.getUserId())
                .map(user -> WorkspaceMemberResponse.of(member, user));
    }

    /**
     * 워크스페이스 멤버 제거
     */
    public Mono<Void> removeMember(
            String workspaceId,
            String targetMemberId,
            String requesterId) {
        return validateAdminAccess(workspaceId, requesterId)
                .then(findWorkspaceMemberByMemberIdAndWorkspaceId(
                        targetMemberId, workspaceId))
                .flatMap(targetMember -> modifyMemberWithAdminGuard(workspaceId,
                        targetMember, WorkspaceMember::delete))
                .then()
                .as(transactionalOperator::transactional);
    }

    /**
     * 본인 워크스페이스 탈퇴
     */
    public Mono<Void> leaveMember(
            String workspaceId,
            String targetUserId) {
        return findWorkspaceMemberByUserIdAndWorkspaceId(targetUserId,
                workspaceId)
                .flatMap(member -> workspaceMemberRepository
                        .countByWorkspaceIdAndNotDeleted(workspaceId)
                        .flatMap(totalMembers -> {
                            if (totalMembers == 1) {
                                return this.deleteWorkspace(workspaceId,
                                        targetUserId);
                            }

                            return modifyMemberWithAdminGuard(
                                    workspaceId,
                                    member,
                                    WorkspaceMember::delete);
                        }))
                .then()
                .as(transactionalOperator::transactional);
    }

    /**
     * 멤버 권한 변경
     */
    public Mono<WorkspaceMemberResponse> updateMemberRole(
            String workspaceId,
            String memberId,
            UpdateMemberRoleRequest request,
            String currentUserId) {
        return validateAdminAccess(workspaceId, currentUserId)
                .then(findWorkspaceMemberByMemberIdAndWorkspaceId(memberId,
                        workspaceId))
                .flatMap(targetMember -> modifyMemberWithAdminGuard(workspaceId,
                        targetMember, m -> m.updateRole(request.role())))
                .flatMap(this::buildMemberResponse)
                .as(transactionalOperator::transactional);
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
        return findWorkspaceMemberByUserIdAndWorkspaceId(userId, workspaceId)
                .flatMap(member -> {
                    if (!member.isAdmin()) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ADMIN_REQUIRED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<WorkspaceMember> modifyMemberWithAdminGuard(
            String workspaceId,
            WorkspaceMember member,
            Consumer<WorkspaceMember> action) {
        if (!member.isAdmin()) {
            action.accept(member);
            return workspaceMemberRepository.save(member);
        }

        return workspaceMemberRepository
                .countByWorkspaceIdAndRoleAndNotDeleted(workspaceId,
                        WorkspaceRole.ADMIN.getValue())
                .flatMap(count -> {
                    if (count <= 1) {
                        return Mono.error(new BusinessException(
                                ErrorCode.LAST_ADMIN_CANNOT_LEAVE));
                    }
                    action.accept(member);
                    return workspaceMemberRepository.save(member);
                })
                .retryWhen(Retry.max(3)
                        .filter(OptimisticLockingFailureException.class::isInstance));
    }

    private Mono<WorkspaceMember> findWorkspaceMemberByUserIdAndWorkspaceId(
            String userId, String workspaceId) {
        return workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.WORKSPACE_MEMBER_NOT_FOUND)));
    }

    private Mono<WorkspaceMember> findWorkspaceMemberByMemberIdAndWorkspaceId(
            String memberId, String workspaceId) {
        return workspaceMemberRepository
                .findByIdAndWorkspaceIdAndNotDeleted(memberId, workspaceId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(
                                ErrorCode.WORKSPACE_MEMBER_NOT_FOUND)));
    }

    private void validateSettings(WorkspaceSettings settings) {
        settings.validate();
        String json = settings.toJson();
        if (json.length() > 65536) {
            throw new BusinessException(ErrorCode.WORKSPACE_SETTINGS_TOO_LARGE);
        }
    }

}

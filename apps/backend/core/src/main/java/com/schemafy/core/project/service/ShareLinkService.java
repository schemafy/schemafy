package com.schemafy.core.project.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.controller.dto.request.CreateShareLinkRequest;
import com.schemafy.core.project.controller.dto.response.ShareLinkAccessResponse;
import com.schemafy.core.project.controller.dto.response.ShareLinkResponse;
import com.schemafy.core.project.repository.ShareLinkAccessLogRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.ShareLinkAccessLog;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkAccessLogRepository accessLogRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ShareLinkTokenService tokenService;

    @Transactional
    public Mono<ShareLinkResponse> createShareLink(String workspaceId,
            String projectId, CreateShareLinkRequest request, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectOwnerAccess(workspaceId, projectId,
                        userId))
                .then(Mono.defer(() -> {
                    String token = tokenService.generateToken();
                    byte[] tokenHash = tokenService.hashToken(token);

                    ShareLinkRole role = ShareLinkRole
                            .fromString(request.role());
                    Instant expiresAt = request.expiresAt();

                    ShareLink shareLink = ShareLink.create(projectId, tokenHash,
                            role, expiresAt);

                    return shareLinkRepository.save(shareLink)
                            .map(saved -> ShareLinkResponse.of(saved, token));
                }));
    }

    @Transactional(readOnly = true)
    public Mono<PageResponse<ShareLinkResponse>> getShareLinks(
            String workspaceId,
            String projectId, String userId, int page, int size) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(validateProjectOwnerAccess(workspaceId, projectId,
                        userId))
                .then(shareLinkRepository
                        .countByProjectIdAndNotDeleted(projectId))
                .flatMap(total -> {
                    int offset = page * size;
                    return shareLinkRepository
                            .findByProjectIdAndNotDeleted(projectId, size,
                                    offset)
                            .map(ShareLinkResponse::from)
                            .collectList()
                            .map(list -> PageResponse.of(list, page, size,
                                    total));
                });
    }

    @Transactional(readOnly = true)
    public Mono<ShareLinkResponse> getShareLink(String workspaceId,
            String projectId, String shareLinkId, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND)))
                .flatMap(shareLink -> {
                    // Verify URL projectId matches share link's project
                    if (!shareLink.getProjectId().equals(projectId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_INPUT_VALUE));
                    }
                    // Validate ownership of the share link's actual project
                    return validateProjectOwnerAccess(workspaceId,
                            shareLink.getProjectId(), userId)
                            .thenReturn(ShareLinkResponse.from(shareLink));
                });
    }

    @Transactional
    public Mono<Void> revokeShareLink(String workspaceId, String projectId,
            String shareLinkId, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND)))
                .flatMap(shareLink -> {
                    // Verify URL projectId matches share link's project
                    if (!shareLink.getProjectId().equals(projectId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_INPUT_VALUE));
                    }
                    // Validate ownership of the share link's actual project
                    return validateProjectOwnerAccess(workspaceId,
                            shareLink.getProjectId(), userId)
                            .then(Mono.defer(() -> {
                                shareLink.revoke();
                                return shareLinkRepository.save(shareLink).then();
                            }));
                });
    }

    @Transactional
    public Mono<Void> deleteShareLink(String workspaceId, String projectId,
            String shareLinkId, String userId) {
        return validateWorkspaceMemberAccess(workspaceId, userId)
                .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND)))
                .flatMap(shareLink -> {
                    // Verify URL projectId matches share link's project
                    if (!shareLink.getProjectId().equals(projectId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_INPUT_VALUE));
                    }
                    // Validate ownership of the share link's actual project
                    return validateProjectOwnerAccess(workspaceId,
                            shareLink.getProjectId(), userId)
                            .then(Mono.defer(() -> {
                                shareLink.delete();
                                return shareLinkRepository.save(shareLink).then();
                            }));
                });
    }

    @Transactional
    public Mono<ShareLinkAccessResponse> accessByToken(String token,
            String userId, String ipAddress, String userAgent) {
        byte[] tokenHash = tokenService.hashToken(token);

        return shareLinkRepository.findValidByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SHARE_LINK_INVALID)))
                .flatMap(shareLink -> {
                    recordAccessAsync(shareLink.getId(), userId, ipAddress,
                            userAgent);

                    return projectRepository
                            .findByIdAndNotDeleted(shareLink.getProjectId())
                            .switchIfEmpty(Mono.error(
                                    new BusinessException(ErrorCode.NOT_FOUND)))
                            .map(project -> {
                                ShareLinkRole effectiveRole = (userId == null)
                                        ? ShareLinkRole.VIEWER
                                        : shareLink.getRoleAsEnum();
                                return ShareLinkAccessResponse.of(project,
                                        effectiveRole);
                            });
                });
    }

    private void recordAccessAsync(String shareLinkId, String userId,
            String ipAddress, String userAgent) {
        ShareLinkAccessLog accessLog = ShareLinkAccessLog.create(shareLinkId,
                userId, ipAddress, userAgent);

        Mono.zip(accessLogRepository.save(accessLog),
                shareLinkRepository.incrementAccessCount(shareLinkId))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> log.debug("Access logged for share link: {}",
                                shareLinkId),
                        error -> log.error(
                                "Failed to log access for share link: {}",
                                shareLinkId, error));
    }

    private Mono<Void> validateWorkspaceMemberAccess(String workspaceId,
            String userId) {
        return workspaceMemberRepository
                .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new BusinessException(
                                ErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateProjectOwnerAccess(String workspaceId,
            String projectId, String userId) {
        return projectRepository.findByIdAndNotDeleted(projectId)
                .switchIfEmpty(
                        Mono.error(new BusinessException(ErrorCode.NOT_FOUND)))
                .flatMap(project -> {
                    if (!project.belongsToWorkspace(workspaceId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.INVALID_INPUT_VALUE));
                    }
                    if (!project.isOwner(userId)) {
                        return Mono.error(new BusinessException(
                                ErrorCode.ACCESS_DENIED));
                    }
                    return Mono.empty();
                });
    }

}

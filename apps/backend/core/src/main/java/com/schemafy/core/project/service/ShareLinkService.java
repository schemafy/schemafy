package com.schemafy.core.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.type.PageResponse;
import com.schemafy.core.project.controller.dto.request.CreateShareLinkRequest;
import com.schemafy.core.project.controller.dto.response.ShareLinkAccessResponse;
import com.schemafy.core.project.controller.dto.response.ShareLinkResponse;
import com.schemafy.core.project.exception.ProjectErrorCode;
import com.schemafy.core.project.exception.ShareLinkErrorCode;
import com.schemafy.core.project.exception.WorkspaceErrorCode;
import com.schemafy.core.project.repository.*;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.ShareLink;
import com.schemafy.core.project.repository.entity.ShareLinkAccessLog;
import com.schemafy.core.project.repository.vo.ShareLinkRole;
import com.schemafy.domain.common.exception.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkService {

  private final TransactionalOperator transactionalOperator;
  private final ShareLinkRepository shareLinkRepository;
  private final ShareLinkAccessLogRepository accessLogRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ShareLinkTokenService tokenService;

  public Mono<ShareLinkResponse> createShareLink(String workspaceId,
      String projectId, CreateShareLinkRequest request, String userId) {
    return validateWorkspaceMember(workspaceId, userId)
        .then(validateProjectAdmin(workspaceId, projectId,
            userId))
        .then(Mono.defer(() -> {
          String token = tokenService.generateToken();
          byte[] tokenHash = tokenService.hashToken(token);

          ShareLinkRole role = ShareLinkRole
              .fromString(request.role());

          ShareLink shareLink = ShareLink.create(projectId, tokenHash,
              role, request.expiresAt());

          return shareLinkRepository.save(shareLink)
              .map(saved -> ShareLinkResponse.of(saved, token));
        }))
        .as(transactionalOperator::transactional);
  }

  public Mono<PageResponse<ShareLinkResponse>> getShareLinks(
      String workspaceId,
      String projectId, String userId, int page, int size) {
    return validateWorkspaceMember(workspaceId, userId)
        .then(validateProjectAdmin(workspaceId, projectId,
            userId))
        .then(shareLinkRepository
            .countByProjectIdAndNotDeleted(projectId))
        .flatMap(total -> shareLinkRepository
            .findByProjectIdAndNotDeleted(projectId, size,
                page * size)
            .map(ShareLinkResponse::from)
            .collectList()
            .map(list -> PageResponse.of(list, page, size,
                total)));
  }

  public Mono<ShareLinkResponse> getShareLink(String workspaceId,
      String projectId, String shareLinkId, String userId) {
    return validateWorkspaceMember(workspaceId, userId)
        .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)))
        .flatMap(shareLink -> {
          if (!shareLink.getProjectId().equals(projectId)) {
            return Mono.error(new DomainException(
                ProjectErrorCode.WORKSPACE_MISMATCH));
          }
          return validateProjectAdmin(workspaceId,
              shareLink.getProjectId(), userId)
              .thenReturn(ShareLinkResponse.from(shareLink));
        });
  }

  public Mono<Void> revokeShareLink(String workspaceId, String projectId,
      String shareLinkId, String userId) {
    return validateWorkspaceMember(workspaceId, userId)
        .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)))
        .flatMap(shareLink -> {
          if (!shareLink.getProjectId().equals(projectId)) {
            return Mono.error(new DomainException(
                ProjectErrorCode.WORKSPACE_MISMATCH));
          }
          return validateProjectAdmin(workspaceId,
              shareLink.getProjectId(), userId)
              .then(Mono.defer(() -> {
                shareLink.revoke();
                return shareLinkRepository.save(shareLink)
                    .then();
              }));
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<Void> deleteShareLink(String workspaceId, String projectId,
      String shareLinkId, String userId) {
    return validateWorkspaceMember(workspaceId, userId)
        .then(shareLinkRepository.findByIdAndNotDeleted(shareLinkId))
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)))
        .flatMap(shareLink -> {
          if (!shareLink.getProjectId().equals(projectId)) {
            return Mono.error(new DomainException(
                ProjectErrorCode.WORKSPACE_MISMATCH));
          }
          return validateProjectAdmin(workspaceId,
              shareLink.getProjectId(), userId)
              .then(Mono.defer(() -> {
                shareLink.delete();
                return shareLinkRepository.save(shareLink)
                    .then();
              }));
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<ShareLinkAccessResponse> accessByToken(String token,
      String userId, String ipAddress, String userAgent) {
    byte[] tokenHash = tokenService.hashToken(token);

    return shareLinkRepository.findValidByTokenHash(tokenHash)
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.INVALID)))
        .flatMap(shareLink -> {
          ShareLinkAccessLog accessLog = ShareLinkAccessLog.create(
              shareLink.getId(), userId, ipAddress, userAgent);

          Mono<Project> fetchProject = projectRepository
              .findByIdAndNotDeleted(shareLink.getProjectId())
              .switchIfEmpty(Mono.error(
                  new DomainException(
                      ProjectErrorCode.NOT_FOUND)));

          Mono<Void> recordAccess = Mono.when(
              accessLogRepository.save(accessLog),
              shareLinkRepository
                  .incrementAccessCount(shareLink.getId()))
              .doOnError(
                  e -> log.error("Failed to log access: {}",
                      shareLink.getId(), e))
              .onErrorResume(e -> Mono.empty());

          return fetchProject
              .delayUntil(project -> recordAccess)
              .map(project -> {
                ShareLinkRole effectiveRole = (userId == null)
                    ? ShareLinkRole.VIEWER
                    : shareLink.getRoleAsEnum();
                return ShareLinkAccessResponse.of(project,
                    effectiveRole);
              });
        });
  }

  private Mono<Boolean> validateWorkspaceMember(String workspaceId,
      String userId) {
    return workspaceMemberRepository
        .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .flatMap(exists -> {
          if (!exists) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.ACCESS_DENIED));
          }
          return Mono.empty();
        });
  }

  private Mono<Void> validateProjectAdmin(String workspaceId,
      String projectId, String userId) {
    return projectRepository.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.NOT_FOUND)))
        .filter(project -> project.belongsToWorkspace(workspaceId))
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.WORKSPACE_MISMATCH)))
        .flatMap(project -> projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(projectId,
                userId))
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .filter(ProjectMember::isAdmin)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.ADMIN_REQUIRED)))
        .then();
  }

}

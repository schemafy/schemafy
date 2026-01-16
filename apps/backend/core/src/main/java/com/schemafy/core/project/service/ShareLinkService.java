package com.schemafy.core.project.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.project.repository.ProjectMemberRepository;
import com.schemafy.core.project.repository.ProjectRepository;
import com.schemafy.core.project.repository.ShareLinkRepository;
import com.schemafy.core.project.repository.WorkspaceMemberRepository;
import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.entity.ProjectMember;
import com.schemafy.core.project.repository.entity.ShareLink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareLinkService {

  private final ShareLinkRepository shareLinkRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final TransactionalOperator transactionalOperator;

  public Mono<Project> accessByCode(String code, String userId, String ipAddress, String userAgent) {
    String user = userId != null ? userId : "anonymous";

    return shareLinkRepository.findValidLinkByCode(code, Instant.now())
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SHARE_LINK_INVALID)))
        .doOnNext(shareLink -> log.info(
            "ShareLink access success - code: {}, projectId: {}, userId: {}, ip: {}, userAgent: {}",
            code, shareLink.getProjectId(), user, ipAddress, userAgent))
        .flatMap(shareLink -> shareLinkRepository.incrementAccessCount(shareLink.getId())
            .onErrorResume(e -> {
              log.error("Failed to increment access count for ShareLink id: {}", shareLink.getId(), e);
              return Mono.empty();
            })
            .then(projectRepository.findByIdAndNotDeleted(shareLink.getProjectId()))
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROJECT_NOT_FOUND))))
        .doOnError(ex -> log.info("ShareLink access failed - code: {}, userId: {}, ip: {}, userAgent: {}, reason: {}",
            code, user, ipAddress, userAgent, ex.getMessage()));
  }

  public Mono<ShareLink> createShareLink(String workspaceId, String projectId, String userId) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .then(findProjectById(projectId))
        .flatMap(project -> {
          if (!project.getWorkspaceId().equals(workspaceId)) {
            return Mono.error(new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
          }

          String code = UUID.randomUUID().toString().replace("-", "");
          ShareLink shareLink = ShareLink.create(projectId, code);

          return shareLinkRepository.save(shareLink);
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<Long> countShareLinks(String workspaceId, String projectId, String userId) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .then(shareLinkRepository.countByProjectIdAndNotDeleted(projectId));
  }

  public Flux<ShareLink> getShareLinks(
      String workspaceId, String projectId, String userId, int size, int offset) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .thenMany(shareLinkRepository.findByProjectIdAndNotDeleted(projectId, size, offset));
  }

  public Mono<ShareLink> getShareLink(
      String workspaceId, String projectId, String shareLinkId, String userId) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .then(findShareLinkById(shareLinkId, projectId));
  }

  public Mono<ShareLink> revokeShareLink(
      String workspaceId, String projectId, String shareLinkId, String userId) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .then(findShareLinkById(shareLinkId, projectId))
        .flatMap(shareLink -> {
          if (shareLink.getIsRevoked()) {
            return Mono.error(new BusinessException(ErrorCode.SHARE_LINK_REVOKED));
          }
          shareLink.revoke();
          return shareLinkRepository.save(shareLink);
        })
        .as(transactionalOperator::transactional);
  }

  public Mono<Void> deleteShareLink(
      String workspaceId, String projectId, String shareLinkId, String userId) {
    return validateAdminAccess(workspaceId, projectId, userId)
        .then(findShareLinkById(shareLinkId, projectId))
        .flatMap(link -> {
          link.delete();
          return shareLinkRepository.save(link).then();
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<ShareLink> findShareLinkById(String shareLinkId, String projectId) {
    return shareLinkRepository.findByIdAndProjectIdAndNotDeleted(shareLinkId, projectId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SHARE_LINK_NOT_FOUND)));
  }

  private Mono<Project> findProjectById(String projectId) {
    return projectRepository.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROJECT_NOT_FOUND)));
  }

  private Mono<Void> validateAdminAccess(String workspaceId, String projectId, String userId) {
    return workspaceMemberRepository
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED)))
        .then(projectMemberRepository
            .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED)))
            .filter(ProjectMember::isAdmin)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PROJECT_ADMIN_REQUIRED))))
        .then();
  }

}

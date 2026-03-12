package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.out.ProjectMemberPort;
import com.schemafy.domain.project.application.port.out.ProjectPort;
import com.schemafy.domain.project.application.port.out.ShareLinkPort;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ProjectMember;
import com.schemafy.domain.project.domain.ShareLink;
import com.schemafy.domain.project.domain.exception.ProjectErrorCode;
import com.schemafy.domain.project.domain.exception.ShareLinkErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ShareLinkHelper {

  private final ShareLinkPort shareLinkPort;
  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;

  Mono<ShareLink> validateShareLinkAccessible(ShareLink shareLink) {
    if (Boolean.TRUE.equals(shareLink.getIsRevoked()) || shareLink.isExpired()) {
      return Mono.error(new DomainException(ShareLinkErrorCode.INVALID_LINK));
    }

    return Mono.just(shareLink);
  }

  Mono<ShareLink> findShareLinkById(String shareLinkId, String projectId) {
    return shareLinkPort.findByIdAndProjectIdAndNotDeleted(shareLinkId,
        projectId)
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)));
  }

  Mono<Project> findProjectById(String projectId) {
    return projectPort.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.NOT_FOUND)));
  }

  Mono<Void> validateAdminAccess(String projectId, String userId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .filter(ProjectMember::isAdmin)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ADMIN_REQUIRED)))
        .then();
  }

}

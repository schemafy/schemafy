package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ShareLinkHelper {

  private final ShareLinkPort shareLinkPort;
  private final ProjectPort projectPort;

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

}

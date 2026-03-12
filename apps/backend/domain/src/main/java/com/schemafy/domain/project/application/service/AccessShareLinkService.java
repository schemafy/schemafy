package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.domain.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.domain.project.application.port.out.ShareLinkPort;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.exception.ProjectErrorCode;
import com.schemafy.domain.project.domain.exception.ShareLinkErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class AccessShareLinkService implements AccessShareLinkUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      AccessShareLinkService.class);

  private final ShareLinkPort shareLinkPort;
  private final ShareLinkHelper shareLinkHelper;

  @Override
  public Mono<Project> accessShareLink(AccessShareLinkQuery query) {
    String user = query.userId() != null ? query.userId() : "anonymous";

    return shareLinkPort.findByCodeAndNotDeleted(query.code())
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)))
        .flatMap(shareLinkHelper::validateShareLinkAccessible)
        .doOnNext(shareLink -> log.info(
            "ShareLink access success - code: {}, projectId: {}, userId: {}, ip: {}, userAgent: {}",
            maskCode(query.code()), shareLink.getProjectId(), user,
            query.ipAddress(), query.userAgent()))
        .flatMap(shareLink -> shareLinkPort.incrementAccessCount(
            shareLink.getId())
            .onErrorResume(error -> {
              log.error(
                  "Failed to increment access count for ShareLink InvitationId: {}",
                  shareLink.getId(), error);
              return Mono.empty();
            })
            .then(shareLinkHelper.findProjectById(shareLink.getProjectId()))
            .switchIfEmpty(Mono.error(
                new DomainException(ProjectErrorCode.NOT_FOUND))))
        .doOnError(error -> log.info(
            "ShareLink access failed - code: {}, userId: {}, ip: {}, userAgent: {}, reason: {}",
            maskCode(query.code()), user, query.ipAddress(),
            query.userAgent(), error.getMessage()));
  }

  private String maskCode(String code) {
    if (code == null || code.length() <= 4) {
      return "***";
    }
    return code.substring(0, Math.min(8, code.length())) + "***";
  }

}

package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.in.AccessShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;

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
    return shareLinkPort.findByIdAndNotDeleted(query.shareLinkId())
        .switchIfEmpty(Mono.error(
            new DomainException(ShareLinkErrorCode.NOT_FOUND)))
        .flatMap(shareLinkHelper::validateShareLinkAccessible)
        .flatMap(shareLink -> shareLinkHelper.findProjectById(shareLink.getProjectId())
            .switchIfEmpty(Mono.error(
                new DomainException(ProjectErrorCode.NOT_FOUND)))
            .doOnNext(project -> log.info("event=share_link_access projectId={} shareLinkId={} ip={} userAgent={}",
                shareLink.getProjectId(), shareLink.getId(), query.ipAddress(), query.userAgent())));
  }

}

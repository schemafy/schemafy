package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.port.in.GetShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetShareLinkUseCase;
import com.schemafy.core.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetShareLinkService implements GetShareLinkUseCase {

  private final ShareLinkHelper shareLinkHelper;

  @Override
  public Mono<ShareLink> getShareLink(GetShareLinkQuery query) {
    return shareLinkHelper.validateAdminAccess(query.projectId(),
        query.requesterId())
        .then(shareLinkHelper.findShareLinkById(query.shareLinkId(),
            query.projectId()));
  }

}

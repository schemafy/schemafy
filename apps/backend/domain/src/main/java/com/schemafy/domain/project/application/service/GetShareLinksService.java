package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.PageResult;
import com.schemafy.domain.project.application.port.in.GetShareLinksQuery;
import com.schemafy.domain.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.domain.project.application.port.out.ShareLinkPort;
import com.schemafy.domain.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetShareLinksService implements GetShareLinksUseCase {

  private final ShareLinkPort shareLinkPort;
  private final ShareLinkHelper shareLinkHelper;

  @Override
  public Mono<PageResult<ShareLink>> getShareLinks(GetShareLinksQuery query) {
    int offset = query.page() * query.size();
    return shareLinkHelper.validateAdminAccess(query.projectId(),
        query.requesterId())
        .then(shareLinkPort.countByProjectIdAndNotDeleted(query.projectId()))
        .flatMap(totalElements -> shareLinkPort
            .findByProjectIdAndNotDeleted(query.projectId(), query.size(),
                offset)
            .collectList()
            .map(links -> PageResult.of(links, query.page(), query.size(),
                totalElements)));
  }

}

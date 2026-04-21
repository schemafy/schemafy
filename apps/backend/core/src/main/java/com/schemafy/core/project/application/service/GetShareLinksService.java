package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.PageResult;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.GetShareLinksQuery;
import com.schemafy.core.project.application.port.in.GetShareLinksUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetShareLinksService implements GetShareLinksUseCase {

  private final ShareLinkPort shareLinkPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<PageResult<ShareLink>> getShareLinks(GetShareLinksQuery query) {
    int offset = query.page() * query.size();
    return shareLinkPort.countByProjectIdAndNotDeleted(query.projectId())
        .flatMap(totalElements -> shareLinkPort
            .findByProjectIdAndNotDeleted(query.projectId(), query.size(),
                offset)
            .collectList()
            .map(links -> PageResult.of(links, query.page(), query.size(),
                totalElements)));
  }

}

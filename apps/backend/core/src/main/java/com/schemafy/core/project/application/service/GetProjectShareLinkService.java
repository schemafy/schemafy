package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkQuery;
import com.schemafy.core.project.application.port.in.GetProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetProjectShareLinkService implements GetProjectShareLinkUseCase {

  private final ShareLinkHelper shareLinkHelper;
  private final ShareLinkPort shareLinkPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ShareLink> getProjectShareLink(GetProjectShareLinkQuery query) {
    return shareLinkHelper.findProjectById(query.projectId())
        .then(shareLinkPort.findByProjectIdAndNotDeleted(query.projectId()));
  }

}

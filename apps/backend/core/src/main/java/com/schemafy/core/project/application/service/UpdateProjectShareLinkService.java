package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateProjectShareLinkService implements UpdateProjectShareLinkUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final ShareLinkHelper shareLinkHelper;
  private final ShareLinkPort shareLinkPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ShareLink> updateProjectShareLink(UpdateProjectShareLinkCommand command) {
    return shareLinkHelper.findProjectById(command.projectId())
        .then(shareLinkPort.findByProjectIdAndNotDeleted(command.projectId())
            .flatMap(link -> saveWithActiveState(link, command.isActive()))
            .switchIfEmpty(command.isActive() ? create(command.projectId()) : Mono.empty()));
  }

  private Mono<ShareLink> create(String projectId) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .map(id -> ShareLink.create(id, projectId))
        .flatMap(shareLinkPort::save)
        .onErrorResume(DataIntegrityViolationException.class, error -> shareLinkPort.findByProjectIdAndNotDeleted(
            projectId)
            .switchIfEmpty(Mono.error(error)));
  }

  private Mono<ShareLink> saveWithActiveState(ShareLink link, boolean isActive) {
    if (link.isActive() == isActive) {
      return Mono.just(link);
    }
    if (isActive) {
      link.activate();
    } else {
      link.deactivate();
    }
    return shareLinkPort.save(link);
  }

}

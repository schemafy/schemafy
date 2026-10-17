package com.schemafy.core.project.application.service;

import java.util.UUID;

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
  private final ProjectMutationGuard projectMutationGuard;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ShareLink> updateProjectShareLink(UpdateProjectShareLinkCommand command) {
    return projectMutationGuard.protectChildCreation(command.projectId(),
        () -> shareLinkHelper.findProjectById(command.projectId())
            .then(shareLinkPort.findByProjectIdAndNotDeleted(command.projectId())
                .flatMap(link -> saveWithActiveState(link, command.isActive()))
                .switchIfEmpty(command.isActive() ? create(command.projectId()) : Mono.empty())))
        // A duplicate insert must roll back before reading the winner with a fresh snapshot.
        .onErrorResume(ShareLinkCreationConflictException.class,
            error -> projectMutationGuard.protectChildCreation(command.projectId(),
                () -> shareLinkPort.findByProjectIdAndNotDeleted(command.projectId())
                    .switchIfEmpty(Mono.error(error.getCause()))));
  }

  private Mono<ShareLink> create(String projectId) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .map(id -> ShareLink.create(
            id, projectId, UUID.randomUUID().toString().replace("-", "")))
        .flatMap(shareLinkPort::save)
        .onErrorMap(DataIntegrityViolationException.class, ShareLinkCreationConflictException::new);
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

  private static final class ShareLinkCreationConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ShareLinkCreationConflictException(DataIntegrityViolationException cause) {
      super(cause);
    }

  }

}

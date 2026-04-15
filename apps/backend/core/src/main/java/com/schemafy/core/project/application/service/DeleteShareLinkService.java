package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteShareLinkService implements DeleteShareLinkUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ShareLinkPort shareLinkPort;
  private final ShareLinkHelper shareLinkHelper;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<Void> deleteShareLink(DeleteShareLinkCommand command) {
    return shareLinkHelper.validateAdminAccess(command.projectId(), command.requesterId())
        .then(projectAccessHelper.findProjectById(command.projectId()))
        .flatMap(project -> Mono.defer(() -> projectAccessHelper
            .requireProjectWithinWorkspaceForWrite(
                project.getWorkspaceId(), command.projectId())
            .then(shareLinkHelper.findShareLinkById(command.shareLinkId(),
                command.projectId()))
            .flatMap(link -> {
              link.delete();
              return shareLinkPort.save(link).then();
            })
            .as(transactionalOperator::transactional)));
  }

}

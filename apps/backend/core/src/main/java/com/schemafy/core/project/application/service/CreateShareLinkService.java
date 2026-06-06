package com.schemafy.core.project.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.CreateShareLinkCommand;
import com.schemafy.core.project.application.port.in.CreateShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateShareLinkService implements CreateShareLinkUseCase {

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final ShareLinkPort shareLinkPort;
  private final ShareLinkHelper shareLinkHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ShareLink> createShareLink(CreateShareLinkCommand command) {
    return shareLinkHelper.findProjectById(command.projectId())
        .flatMap(project -> Mono.fromCallable(ulidGeneratorPort::generate)
            .flatMap(id -> {
              String code = UUID.randomUUID().toString().replace("-", "");
              return shareLinkPort.save(
                  ShareLink.create(id, command.projectId(), code));
            }))
        .as(transactionalOperator::transactional);
  }

}

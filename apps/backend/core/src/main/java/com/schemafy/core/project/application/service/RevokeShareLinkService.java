package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.RevokeShareLinkCommand;
import com.schemafy.core.project.application.port.in.RevokeShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RevokeShareLinkService implements RevokeShareLinkUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ShareLinkPort shareLinkPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<ShareLink> revokeShareLink(RevokeShareLinkCommand command) {
    return shareLinkPort.revokeByIdAndProjectId(command.shareLinkId(),
        command.projectId())
        .flatMap(updated -> updated > 0
            ? shareLinkPort.findByIdAndProjectIdAndNotDeleted(
                command.shareLinkId(), command.projectId())
                .switchIfEmpty(Mono.error(new DomainException(
                    ShareLinkErrorCode.NOT_FOUND)))
            : Mono.error(new DomainException(
                ShareLinkErrorCode.NOT_FOUND)))
        .as(transactionalOperator::transactional);
  }

}

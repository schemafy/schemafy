package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.DeleteShareLinkCommand;
import com.schemafy.core.project.application.port.in.DeleteShareLinkUseCase;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.exception.ShareLinkErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteShareLinkService implements DeleteShareLinkUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ShareLinkPort shareLinkPort;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<Void> deleteShareLink(DeleteShareLinkCommand command) {
    return shareLinkPort.softDeleteByIdAndProjectId(command.shareLinkId(),
        command.projectId())
        .flatMap(updated -> updated > 0
            ? Mono.<Void>empty()
            : Mono.<Void>error(new DomainException(
                ShareLinkErrorCode.NOT_FOUND)))
        .as(transactionalOperator::transactional);
  }

}

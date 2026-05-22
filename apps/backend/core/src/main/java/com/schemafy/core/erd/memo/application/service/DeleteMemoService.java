package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoUseCase;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.VIEWER)
class DeleteMemoService implements DeleteMemoUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final ProjectMemberPort projectMemberPort;
  private final SoftDeleteMemoPort softDeleteMemoPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<Void> deleteMemo(DeleteMemoCommand command) {
    return getMemoByIdPort.findMemoById(command.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> canDelete(memo.authorId(), command.requesterId(), memo.schemaId())
            .flatMap(canDelete -> canDelete
                ? softDeleteMemoPort.softDeleteMemo(memo.id(), Instant.now())
                : Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED))))
        .as(transactionalOperator::transactional);
  }

  private Mono<Boolean> canDelete(
      String authorId,
      String requesterId,
      String schemaId) {
    if (authorId.equals(requesterId)) {
      return Mono.just(true);
    }
    return getSchemaByIdPort.findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(
            new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + schemaId)))
        .flatMap(schema -> projectMemberPort
            .findByProjectIdAndUserIdAndNotDeleted(schema.projectId(), requesterId))
        .map(member -> member.getRoleAsEnum().isAdmin())
        .defaultIfEmpty(false);
  }

}

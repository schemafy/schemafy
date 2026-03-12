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

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteMemoService implements DeleteMemoUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final SoftDeleteMemoPort softDeleteMemoPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<Void> deleteMemo(DeleteMemoCommand command) {
    return getMemoByIdPort.findMemoById(command.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> {
          boolean hasPermission = memo.authorId().equals(command.requesterId())
              || command.canDeleteOthers();
          if (!hasPermission) {
            return Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED));
          }
          return softDeleteMemoPort.softDeleteMemo(memo.id(), Instant.now());
        })
        .as(transactionalOperator::transactional);
  }

}

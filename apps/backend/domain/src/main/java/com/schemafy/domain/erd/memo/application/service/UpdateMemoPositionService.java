package com.schemafy.domain.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoPositionCommand;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoPositionUseCase;
import com.schemafy.domain.erd.memo.application.port.out.ChangeMemoPositionPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.domain.Memo;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateMemoPositionService implements UpdateMemoPositionUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final ChangeMemoPositionPort changeMemoPositionPort;

  @Override
  public Mono<Memo> updateMemoPosition(UpdateMemoPositionCommand command) {
    return getMemoByIdPort.findMemoById(command.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> {
          if (!memo.authorId().equals(command.requesterId())) {
            return Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED));
          }
          return changeMemoPositionPort
              .changeMemoPosition(memo.id(), command.positions())
              .then(getMemoByIdPort.findMemoById(memo.id()))
              .switchIfEmpty(
                  Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)));
        });
  }

}

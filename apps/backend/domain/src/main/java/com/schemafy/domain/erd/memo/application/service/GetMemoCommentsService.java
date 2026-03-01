package com.schemafy.domain.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoCommentsQuery;
import com.schemafy.domain.erd.memo.application.port.in.GetMemoCommentsUseCase;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class GetMemoCommentsService implements GetMemoCommentsUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final GetMemoCommentsByMemoIdPort getMemoCommentsByMemoIdPort;

  @Override
  public Flux<MemoComment> getMemoComments(GetMemoCommentsQuery query) {
    return getMemoByIdPort.findMemoById(query.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .thenMany(getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId(
            query.memoId()));
  }

}

package com.schemafy.domain.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommentCommand;
import com.schemafy.domain.erd.memo.application.port.in.CreateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.out.CreateMemoCommentPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateMemoCommentService implements CreateMemoCommentUseCase {

  private final GetMemoByIdPort getMemoByIdPort;
  private final CreateMemoCommentPort createMemoCommentPort;
  private final UlidGeneratorPort ulidGeneratorPort;

  @Override
  public Mono<MemoComment> createMemoComment(CreateMemoCommentCommand command) {
    return getMemoByIdPort.findMemoById(command.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .then(Mono.fromCallable(ulidGeneratorPort::generate)
            .flatMap(commentId -> createMemoCommentPort.createMemoComment(
                new MemoComment(
                    commentId,
                    command.memoId(),
                    command.authorId(),
                    command.body(),
                    null,
                    null,
                    null))));
  }

}

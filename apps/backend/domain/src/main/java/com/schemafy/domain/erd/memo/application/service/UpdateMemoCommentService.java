package com.schemafy.domain.erd.memo.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.out.ChangeMemoCommentBodyPort;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoCommentCommand;
import com.schemafy.domain.erd.memo.application.port.in.UpdateMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.domain.erd.memo.domain.MemoComment;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class UpdateMemoCommentService implements UpdateMemoCommentUseCase {

  private final GetMemoCommentByIdPort getMemoCommentByIdPort;
  private final GetMemoByIdPort getMemoByIdPort;
  private final ChangeMemoCommentBodyPort changeMemoCommentBodyPort;

  @Override
  public Mono<MemoComment> updateMemoComment(UpdateMemoCommentCommand command) {
    return getMemoCommentByIdPort.findMemoCommentById(command.commentId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.COMMENT_NOT_FOUND)))
        .flatMap(comment -> {
          return getMemoByIdPort.findMemoById(comment.memoId())
              .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
              .then(Mono.defer(() -> {
                if (!comment.authorId().equals(command.requesterId())) {
                  return Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED));
                }
                return changeMemoCommentBodyPort
                    .changeMemoCommentBody(comment.id(), command.body())
                    .then(getMemoCommentByIdPort.findMemoCommentById(comment.id()))
                    .switchIfEmpty(Mono.error(
                        new DomainException(MemoErrorCode.COMMENT_NOT_FOUND)));
              }));
        });
  }

}

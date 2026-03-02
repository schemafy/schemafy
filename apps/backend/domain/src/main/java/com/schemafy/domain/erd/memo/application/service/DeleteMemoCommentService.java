package com.schemafy.domain.erd.memo.application.service;

import java.time.Instant;

import com.schemafy.domain.erd.memo.domain.MemoComment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.memo.application.port.out.SoftDeleteMemoCommentPort;
import com.schemafy.domain.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.domain.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.domain.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.domain.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.domain.erd.memo.domain.exception.MemoErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DeleteMemoCommentService implements DeleteMemoCommentUseCase {

  private final GetMemoCommentByIdPort getMemoCommentByIdPort;
  private final GetMemoByIdPort getMemoByIdPort;
  private final GetMemoCommentsByMemoIdPort getMemoCommentsByMemoIdPort;
  private final SoftDeleteMemoCommentPort softDeleteMemoCommentPort;
  private final SoftDeleteMemoPort softDeleteMemoPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<Void> deleteMemoComment(DeleteMemoCommentCommand command) {
    return getMemoCommentByIdPort.findMemoCommentById(command.commentId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.COMMENT_NOT_FOUND)))
        .flatMap(comment -> checkDeletePermission(comment.authorId(), command.requesterId(),
            command.canDeleteOthers())
            .then(Mono.defer(
                () -> findAndDelete(comment, comment.memoId(), command.commentId()))))
        .then()
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> checkDeletePermission(
      String authorId,
      String requesterId,
      boolean canDeleteOthers) {
    if (!authorId.equals(requesterId) && !canDeleteOthers) {
      return Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED));
    }
    return Mono.empty();
  }

  private Mono<Void> findAndDelete(
      MemoComment comment,
      String memoId,
      String commentId) {
    return getMemoByIdPort.findMemoById(memoId)
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> isLastRemainingComment(memoId, commentId)
            .flatMap(isLast -> isLast
                ? softDeleteMemoPort.softDeleteMemo(memo.id(), Instant.now())
                : softDeleteMemoCommentPort.softDeleteMemoComment(
                    comment.id(),
                    Instant.now())));
  }

  private Mono<Boolean> isLastRemainingComment(String memoId, String commentId) {
    return getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId(memoId)
        .collectList()
        .map(comments -> comments.size() == 1
            && comments.getFirst().id().equals(commentId))
        .defaultIfEmpty(false);
  }

}

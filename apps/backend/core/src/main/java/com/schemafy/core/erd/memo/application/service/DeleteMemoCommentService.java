package com.schemafy.core.erd.memo.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentCommand;
import com.schemafy.core.erd.memo.application.port.in.DeleteMemoCommentUseCase;
import com.schemafy.core.erd.memo.application.port.out.GetMemoByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentByIdPort;
import com.schemafy.core.erd.memo.application.port.out.GetMemoCommentsByMemoIdPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoCommentPort;
import com.schemafy.core.erd.memo.application.port.out.SoftDeleteMemoPort;
import com.schemafy.core.erd.memo.domain.MemoComment;
import com.schemafy.core.erd.memo.domain.exception.MemoErrorCode;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequireProjectAccess(role = ProjectRole.VIEWER, target = "memoComment:commentId")
@RequiredArgsConstructor
class DeleteMemoCommentService implements DeleteMemoCommentUseCase {

  private final GetMemoCommentByIdPort getMemoCommentByIdPort;
  private final GetMemoByIdPort getMemoByIdPort;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final ProjectMemberPort projectMemberPort;
  private final GetMemoCommentsByMemoIdPort getMemoCommentsByMemoIdPort;
  private final SoftDeleteMemoCommentPort softDeleteMemoCommentPort;
  private final SoftDeleteMemoPort softDeleteMemoPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<Void> deleteMemoComment(DeleteMemoCommentCommand command) {
    return getMemoCommentByIdPort.findMemoCommentById(command.commentId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.COMMENT_NOT_FOUND)))
        .flatMap(comment -> findAndDelete(
            comment,
            command.requesterId(),
            command.commentId()))
        .then()
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> findAndDelete(
      MemoComment comment,
      String requesterId,
      String commentId) {
    return getMemoByIdPort.findMemoById(comment.memoId())
        .switchIfEmpty(Mono.error(new DomainException(MemoErrorCode.NOT_FOUND)))
        .flatMap(memo -> canDelete(comment.authorId(), requesterId, memo.schemaId())
            .flatMap(canDelete -> canDelete
                ? isLastRemainingComment(memo.id(), commentId)
                    .flatMap(isLast -> isLast
                        ? softDeleteMemoPort.softDeleteMemo(memo.id(), Instant.now())
                        : softDeleteMemoCommentPort.softDeleteMemoComment(
                            comment.id(),
                            Instant.now()))
                : Mono.error(new DomainException(MemoErrorCode.ACCESS_DENIED))));
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

  private Mono<Boolean> isLastRemainingComment(String memoId, String commentId) {
    return getMemoCommentsByMemoIdPort.findMemoCommentsByMemoId(memoId)
        .collectList()
        .map(comments -> comments.size() == 1
            && comments.getFirst().id().equals(commentId))
        .defaultIfEmpty(false);
  }

}
